package com.maciekjanusz.tale;

import android.util.Log;

import static android.util.Log.*;

/**
 * This class is the Android log helper, that enables simple logging without need of
 * setting tags and passing them to every single call of the log method, instead it uses
 * stack trace to show you from where your log method has been called.
 *
 * Usage:
 *
 * new Tale("Log message").at().tell();
 * - this will result in log with message "Log message" and tag pointing to method and class
 * where this Tale was used.
 */
public class Tale {

    /**
     * This constant indicates the stack trace element index that points to
     * the class and method from which you call the {@link #Tale()} / {@link #Tale(String)} constructor,
     * {@link #at()} or {@link #here()} methods.
     */
    private static final int TRACE_DEPTH = 3;
    /**
     * A decoded class name in the format:
     * MainActivity{SomeInnerClass{Runnable(1)}}:
     */
    private String className;
    /**
     * A name of the method that enclosed call to {@link #at()} method.
     */
    private String methodName;
    /**
     * A log message set through {@link #Tale()} constructor or {@link #story(String)} method.
     */
    private String story;
    /**
     * A tag set by {@link #tag(String)} method. If null, the className will be used as a tag,
     * if non-null, this will be used as a tag instead.
     */
    private String tag;

    /**
     * A log-level for logcat output.
     */
    private int logLevel = Log.VERBOSE;

    /**
     * Default constructor: automatically sets the className
     */
    public Tale() {
        String name = Thread.currentThread().getStackTrace()[TRACE_DEPTH].getClassName();
        className = decodeClassName(name);
    }

    /**
     * This constructor works like {@link #Tale()}, plus it sets the log message.
     * @param story a log message
     */
    public Tale(String story) {
        this.story = story;
        String name = Thread.currentThread().getStackTrace()[TRACE_DEPTH].getClassName();
        className = decodeClassName(name);
    }

    /**
     * Calling this method will add a name of the method form wherein this is called to log message.
     * @return this Tale object
     */
    public Tale at() {
        methodName = Thread.currentThread().getStackTrace()[TRACE_DEPTH].getMethodName();
        return this;
    }

    /**
     * Calling this method will reset the className tag according to where a call to this method
     * has been done.
     * @return this Tale object
     */
    public Tale here() {
        String name = Thread.currentThread().getStackTrace()[TRACE_DEPTH].getClassName();
        className = decodeClassName(name);
        return this;
    }

    /**
     * Call to this method with a non-null String param will set this string as a tag for logcat.
     * Calling this with null param will reset the tag to {@link #className}.
     * @param tag Tag for the log, or null for resetting tag to {@link #className}
     * @return this Tale object
     */
    public Tale tag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * A method for setting the log level for output
     * @param logLevel {@link android.util.Log} integer constants: VERBOSE, INFO, ERROR...
     * @return this Tale object
     */
    public Tale how(int logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    /**
     * This method sets new log message.
     * @param story a new log message
     * @return this Tale object
     */
    public Tale story(String story) {
        this.story = story;
        return this;
    }

    /**
     * This method causes the log to be executed with parameters specified through building of
     * this Tale object
     */
    public void tell() {
        StringBuilder msgBuilder = new StringBuilder();
        if (methodName != null) {
            msgBuilder.append("@").append(methodName).append("﹕ ");
        }
        if (story != null) {
            msgBuilder.append(story);
        }
        String message = msgBuilder.toString();
        if(message.isEmpty()) {
            message = "...";
        }

        String logTag = tag != null ? tag : trimPackageName(className);

        log(logTag, message, logLevel);
    }

    /**
     * Execute the Log.X(TAG, "message") method with specified parameters
     * @param tag log tag
     * @param message log message
     * @param logLevel log level
     */
    private static void log(String tag, String message, int logLevel) {
        switch (logLevel) {
            case DEBUG:
                d(tag, message);
                break;
            case INFO:
                i(tag, message);
                break;
            case WARN:
                w(tag, message);
                break;
            case ERROR:
                e(tag, message);
                break;
            case ASSERT:
                wtf(tag, message);
                break;
            default: //case VERBOSE:
                v(tag, message);
                break;
        }
    }

    /**
     * This method removes the package name from full class name, leaving only simple name.
     * @param fullClassName full class name, ex: com.example.package.MyClass
     * @return trimmed simple class name, ex: MyClass
     */
    private static String trimPackageName(String fullClassName) {
        return fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
    }

    /**
     * This method uses stackTrace to translate ambiguous anonymous class names (ie. $1) to
     * meaningful name representing a class that the anonymous extends or interface that it implements,
     * for example: com.example.package.MainActivity$1$1 would be translated to:
     * MainActivity{Runnable(1){Runnable(1)}}
     * @param className class name to translate
     * @return translated class name
     */
    private static String decodeClassName(String className) {
        try {
            Class<?> klass = Class.forName(className);
            className = trimPackageName(className);
            String[] splitted = className.split("\\$");
            int splittedLength = splitted.length;

            String[] logClassNames = new String[splittedLength];

            for (int i = splittedLength - 1; i >= 0; i--) {
                String split = splitted[i];
                String logClassName;
                if(split.matches("\\d+")) {
                    // is anonymous class
                    if(i < splittedLength - 1) {
                        klass = klass.getEnclosingClass(); // up one enclosing class
                    }
                    String name;
                    Class<?>[] interfaces = klass.getInterfaces();
                    if(interfaces == null || interfaces.length == 0) {
                        name = klass.getSuperclass().getSimpleName();
                    } else {
                        name = interfaces[0].getSimpleName();
                    }
                    logClassName = name + "(" + split + ")";
                } else {
                    logClassName = split;
                }
                logClassNames[i] = logClassName;
            }

            StringBuilder builder = new StringBuilder();
            for (int i = 0, logClassNamesLength = logClassNames.length; i < logClassNamesLength; i++) {
                String name = logClassNames[i];
                builder.append(name);

                int nestedCount = logClassNamesLength - 1;
                if(i < nestedCount) {
                    builder.append("{");
                } else {
                    for(int k = 0; k < nestedCount; k++) {
                        builder.append("}");
                    }
                }
            }

            return builder.toString();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return className;
    }

}