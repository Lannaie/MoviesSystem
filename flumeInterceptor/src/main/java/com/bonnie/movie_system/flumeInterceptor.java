package com.bonnie.movie_system;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.interceptor.Interceptor;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * created by Bonnie on 2022/3/6
 */
public class flumeInterceptor implements Interceptor {

    List<Event> eventList;

    public void initialize() {
        eventList = new ArrayList<Event>();
    }

    public Event intercept(Event event) {
        byte[] old_body = event.getBody();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String old_body_str = new String(old_body);
        String new_body_str = old_body_str.substring(0, old_body_str.lastIndexOf("::")) + "::" + df.format(new Date());
        event.setBody(new_body_str.getBytes());
        return event;
    }

    public List<Event> intercept(List<Event> list) {
        eventList.clear();
        for( Event event : list )
        {
            eventList.add(intercept(event));
        }
        return list;
    }

    public void close() {

    }

    public static class Builder implements Interceptor.Builder{

        public Interceptor build() {
            return new flumeInterceptor();
        }

        public void configure(Context context) {

        }
    }
}
