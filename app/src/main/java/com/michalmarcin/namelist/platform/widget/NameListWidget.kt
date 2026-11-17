package com.michalmarcin.namelist.platform.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.michalmarcin.namelist.App
import com.michalmarcin.namelist.MainActivity
import com.michalmarcin.namelist.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget (F20, ADR-031): a big add button that opens the quick-add form, and the newest people.
 * Plain `RemoteViews`, so there is no new dependency. The data is read on each update; the app also
 * asks for an update whenever it leaves the screen, so a new person shows up right away.
 */
class NameListWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, manager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        update(context, manager, intArrayOf(appWidgetId))
    }

    private fun update(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val people = (app as App).container.peopleRepository.observeEveryone().first()
                val content = WidgetContent.from(people)
                for (id in ids) {
                    manager.updateAppWidget(id, build(app, sizeOf(manager, id), content))
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** The widget asks the app to open the quick-add form with this action. */
        const val ACTION_ADD_PERSON = "com.michalmarcin.namelist.action.ADD_PERSON"

        /** Updates every widget on the home screen. Cheap when there is none. */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NameListWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(context, NameListWidget::class.java))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
            )
        }

        private fun sizeOf(manager: AppWidgetManager, id: Int): WidgetSize {
            val options = manager.getAppWidgetOptions(id)
            return WidgetSize.forDp(
                widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250),
                heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110),
            )
        }

        /** The views for one widget. Public for tests. */
        fun build(context: Context, size: WidgetSize, content: WidgetContent): RemoteViews {
            val views = RemoteViews(
                context.packageName,
                when (size) {
                    WidgetSize.ICON -> R.layout.widget_icon
                    WidgetSize.STRIP -> R.layout.widget_strip
                    WidgetSize.CARD -> R.layout.widget_card
                },
            )
            views.setOnClickPendingIntent(R.id.widget_add, addPendingIntent(context))
            if (size != WidgetSize.ICON) {
                views.setOnClickPendingIntent(R.id.widget_text, openPendingIntent(context))
            }
            when (size) {
                WidgetSize.ICON -> Unit
                WidgetSize.STRIP -> {
                    val last = content.lastAdded
                    views.setTextViewText(
                        R.id.widget_subtitle,
                        if (last == null) context.getString(R.string.widget_hint) else context.getString(R.string.widget_last_added, last),
                    )
                }
                WidgetSize.CARD -> {
                    val tiles = intArrayOf(R.id.widget_tile_1, R.id.widget_tile_2, R.id.widget_tile_3)
                    tiles.forEachIndexed { index, tile ->
                        val name = content.recentNames.getOrNull(index)
                        views.setViewVisibility(tile, if (name == null) android.view.View.GONE else android.view.View.VISIBLE)
                        if (name != null) views.setTextViewText(tile, name)
                    }
                    views.setViewVisibility(
                        R.id.widget_recent,
                        if (content.recentNames.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE,
                    )
                }
            }
            return views
        }

        private fun addPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setAction(ACTION_ADD_PERSON)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(context, REQUEST_ADD, intent, FLAGS)
        }

        private fun openPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(context, REQUEST_OPEN, intent, FLAGS)
        }

        private const val REQUEST_ADD = 1
        private const val REQUEST_OPEN = 2
        private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
