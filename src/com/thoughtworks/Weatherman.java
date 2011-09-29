package com.thoughtworks;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Weatherman {
    private MessageSender messageSender = null;

    private final List<URL> zipCodes;
        private final String YAHOO_WEATHER = "http://weather.yahooapis.com/forecastrss?p=";

        public Weatherman(Integer... zips) {
            zipCodes = new ArrayList<URL>(zips.length);
            for (Integer zip : zips) {
                try {
                    zipCodes.add(new URL(YAHOO_WEATHER + zip));
                } catch (Exception e) {
                    // dont add it if it sucks
                }
            }
        }

        public void start() {
            Runnable r = new Runnable() {

                public void run() {
                    int i = 0;
                    while (i >= 0) {
                        int j = i % zipCodes.size();
                        SyndFeedInput input = new SyndFeedInput();
                        try {
                            SyndFeed feed = input.build(new InputStreamReader(zipCodes.get(j).openStream()));
                            SyndEntry entry = (SyndEntry) feed.getEntries().get(0);
                            messageSender.send(entryToHtml(entry));
                            Thread.sleep(30000L);
                        } catch (Exception e) {
                            // just eat it, eat it
                        }
                        i++;
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        }
        private String entryToHtml(SyndEntry entry){
            StringBuilder html = new StringBuilder("<h2>");
            html.append(entry.getTitle());
            html.append("</h2>");
            html.append(entry.getDescription().getValue());
            return html.toString();
        }
}
