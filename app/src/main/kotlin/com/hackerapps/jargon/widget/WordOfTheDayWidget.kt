package com.hackerapps.jargon.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.hackerapps.jargon.R
import com.hackerapps.jargon.data.DictionaryEntry
import com.hackerapps.jargon.data.DictionaryRepository
import com.hackerapps.jargon.data.WordOfTheDay
import com.hackerapps.jargon.ui.MainActivity
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Widget heights (dp) to lay out for on Android 12+, which picks the largest that fits. Only the
// height matters: it decides how many lines of the definition show. Steps of 28dp (about a line
// and a half) keep the unused space under the text small, within RemoteViews' 16-layout limit.
private val LAYOUT_HEIGHTS = (0 until 15).map { 70f + it * 28f }

// Below this height the label and subtitle are dropped so the term and some definition still fit.
private const val COMPACT_HEIGHT_DP = 110f

/**
 * Home Screen widget showing the [WordOfTheDay]. Plain RemoteViews rather than Glance: Glance runs
 * on WorkManager, whose manifest adds four permissions this app deliberately doesn't hold.
 *
 * The dictionary is read straight from the app's bundled asset on each refresh, so there's no
 * shared storage to keep in sync. The word changes at local midnight via an inexact alarm (no
 * exact-alarm permission needed); time and time zone changes refresh it too.
 */
class WordOfTheDayWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        refresh(context, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                refresh(context, manager.getAppWidgetIds(ComponentName(context, WordOfTheDayWidget::class.java)))
            }
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    private fun refresh(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = try {
                    WordOfTheDay.entryFor(LocalDate.now(), DictionaryRepository(context).loadEntries())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
                val manager = AppWidgetManager.getInstance(context)
                appWidgetIds.forEach { id ->
                    manager.updateAppWidget(id, views(context, entry, manager.getAppWidgetOptions(id)))
                }
                scheduleMidnightRefresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun views(context: Context, entry: DictionaryEntry?, options: Bundle): RemoteViews {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return RemoteViews(LAYOUT_HEIGHTS.associate { SizeF(110f, it) to layout(context, entry, it) })
        }
        // Before Android 12 there's one layout per widget; the minimum height is the portrait one
        // on most launchers, and the safe choice in landscape.
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, COMPACT_HEIGHT_DP.toInt())
        return layout(context, entry, height.toFloat())
    }

    private fun layout(context: Context, entry: DictionaryEntry?, heightDp: Float): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_word_of_the_day)
        val compact = heightDp < COMPACT_HEIGHT_DP
        views.setViewVisibility(R.id.widget_label, if (compact) View.GONE else View.VISIBLE)

        if (entry == null) {
            views.setTextViewText(R.id.widget_term, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_definition, "")
            views.setOnClickPendingIntent(android.R.id.background, openIntent(context, entryId = null))
            return views
        }

        val subtitle = listOfNotNull(entry.pronunciation, entry.partOfSpeech).joinToString("  ·  ")
        val showSubtitle = !compact && subtitle.isNotEmpty()
        views.setTextViewText(R.id.widget_term, entry.term)
        views.setTextViewText(R.id.widget_subtitle, subtitle)
        views.setViewVisibility(R.id.widget_subtitle, if (showSubtitle) View.VISIBLE else View.GONE)
        // Paragraph breaks waste the few lines a widget has; run the definition together.
        views.setTextViewText(R.id.widget_definition, entry.definition.replace(Regex("\\s+"), " "))
        views.setInt(R.id.widget_definition, "setMaxLines", definitionLines(heightDp, !compact, showSubtitle))
        views.setOnClickPendingIntent(android.R.id.background, openIntent(context, entry.id))
        return views
    }

    private fun openIntent(context: Context, entryId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        if (entryId != null) {
            intent.action = MainActivity.ACTION_VIEW_ENTRY
            intent.putExtra(MainActivity.EXTRA_ENTRY_ID, entryId)
        }
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleMidnightRefresh(context: Context) {
        val nextMidnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC, nextMidnight, refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, WordOfTheDayWidget::class.java).setAction(ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val ACTION_REFRESH = "com.hackerapps.jargon.action.REFRESH_WIDGET"
    }
}

/**
 * How many lines of definition fit in a widget [heightDp] tall, from the dimensions in
 * widget_word_of_the_day.xml: 16dp padding top and bottom, the label, the term, the optional
 * subtitle and the gap above the definition, then ~16.5dp per 14sp line (measured on a Pixel 8).
 */
internal fun definitionLines(heightDp: Float, showLabel: Boolean, showSubtitle: Boolean): Int {
    var used = 32f + 4f + 27f + 6f
    if (showLabel) used += 15f
    if (showSubtitle) used += 16f
    return ((heightDp - used) / 16.5f).toInt().coerceIn(1, 30)
}
