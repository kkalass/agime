package de.kalass.android.common.util;

import android.util.Log;

import com.freiheit.fuava.ctprofiler.core.CallTreeProfiler;
import com.freiheit.fuava.ctprofiler.core.Statistics;
import com.freiheit.fuava.ctprofiler.core.TimeKeeper;
import com.freiheit.fuava.ctprofiler.core.impl.ProfilerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by klas on 01.07.15.
 */
public class AProfiler {
    private static final CallTreeProfiler PROFILER = ProfilerFactory.getGlobalProfiler();
    private static final TimeKeeper TIME_KEEPER = ProfilerFactory.getGlobalTimeKeeper();
    private static final BackgroundTask NOOP_BGTASK = new BackgroundTask() {
        @Override
        public void onFinish() {

        }

        @Override
        public void afterBackgroundTask() {

        }
    };


    public interface BackgroundTask {
        void onFinish();

        void afterBackgroundTask();
    }

    public static class BackgroundTaskImpl implements BackgroundTask {
        private final String _name;
        // Attention: written by different thread!
        private AtomicReference<Statistics> statistics = new AtomicReference<>();

        private BackgroundTaskImpl(String name) {
            this._name = "Async: " + name;

        }

        public void onFinish() {
            statistics.set(PROFILER.getStatistics());
        }

        public void afterBackgroundTask() {
            PROFILER.begin(this._name, System.nanoTime());
            PROFILER.end(this._name, System.nanoTime(), statistics.get());
        }
    }

    public static BackgroundTask beforeBackgroundTask(String name) {
        if (PROFILER.isEnabled()) {
            return new BackgroundTaskImpl(name);
        }
        return NOOP_BGTASK;
    }

    public static CallTreeProfiler getProfiler() {
        return PROFILER;
    }
    public static TimeKeeper getTimeKeeper() {
        return TIME_KEEPER;
    }

    public static Object start(String name) {
        PROFILER.clear();
        PROFILER.begin(name, System.nanoTime());
        return name;
    }

    public static Object end(Object startResult) {
        if (! (startResult instanceof String)) {
            return null;
        }

        PROFILER.end(startResult.toString(), System.nanoTime());

        String output = null;
        try {
            output = "\n"+ PROFILER.renderThreadStateAsText(new StringBuilder()).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("PROFILER", output);
        return null;
    }
}
