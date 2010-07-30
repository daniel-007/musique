/*
 * Copyright (c) 2008, 2009, 2010 Denis Tulskiy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tulskiy.musique.playlist;

import com.tulskiy.musique.gui.playlist.SeparatorTrack;
import com.tulskiy.musique.playlist.formatting.Parser;
import com.tulskiy.musique.playlist.formatting.tokens.Expression;

import java.util.LinkedList;

/**
 * Manages playback.
 * <p/>
 * Shuffle algorithm taken from streamer.c from DeadBeef project
 * <p/>
 * Author: Denis Tulskiy
 * Date: Jul 1, 2010
 */
public class PlaybackOrder {
    public enum Order {
        DEFAULT("Default"),
        REPEAT("Repeat"),
        REPEAT_TRACK("Repeat Track"),
        REPEAT_ALBUM("Repeat Album"),
        SHUFFLE("Shuffle"),
        RANDOM("Random");

        private String text;

        Order(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    class QueueTuple {
        Track track;
        Playlist playlist;

        QueueTuple(Track track, Playlist playlist) {
            this.track = track;
            this.playlist = playlist;
        }
    }

    private Playlist playlist;
    private Order order = Order.DEFAULT;
    private LinkedList<QueueTuple> queue = new LinkedList<QueueTuple>();
    private Track lastPlayed;
    private Track plMin, plMax;
    private Expression albumFormat = Parser.parse("%albumArtist% | %date% | %album%");

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setLastPlayed(Track lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public Track getLastPlayed() {
        return lastPlayed;
    }

    public void enqueue(Track track, Playlist playlist) {
        queue.add(new QueueTuple(track, playlist));
        updateQueuePositions();
    }

    private void updateQueuePositions() {
        for (int i = 0; i < queue.size(); i++) {
            queue.get(i).track.setQueuePosition(i + 1);
        }
    }

    public void flushQueue() {
        for (QueueTuple tuple : queue) {
            tuple.track.setQueuePosition(-1);
        }
        queue.clear();
        updateQueuePositions();
    }

    public Track next(Track currentTrack) {
        int index;

        if (!queue.isEmpty()) {
            QueueTuple tuple = queue.poll();
            Track track = tuple.track;
            setPlaylist(tuple.playlist);
            track.setQueuePosition(-1);
            updateQueuePositions();
            return track;
        }

        if (playlist == null || playlist.size() <= 0)
            return null;

        if (lastPlayed != null) {
            if (playlist.contains(lastPlayed)) {
                Track track = lastPlayed;
                lastPlayed = null;
                return track;
            }
        }

        if (currentTrack == null) {
            return playlist.get(0);
        } else {
            index = playlist.indexOf(currentTrack);
            if (index == -1)
                return null;

            int size = playlist.size();

            switch (order) {
                case DEFAULT:
                    return next(index);
                case REPEAT:
                    index = (index + 1) % size;
                    break;
                case REPEAT_TRACK:
                    return currentTrack;
                case REPEAT_ALBUM:
                    String album = (String) albumFormat.eval(currentTrack);

                    Track track = next(index);
                    if (track != null) {
                        if (album.equals(albumFormat.eval(track))) {
                            return track;
                        }
                    }

                    for (int i = index; i >= 0; i--) {
                        track = playlist.get(i);
                        Object value = albumFormat.eval(track);
                        if (!album.equals(value)) {
                            return playlist.get(++i);
                        }
                    }

                    return track;
                case RANDOM:
                    index = (int) (Math.random() * size);
                    break;
                case SHUFFLE:
                    return nextShuffle();

            }
        }

        return getTrack(index);
    }

    private Track next(int index) {
        return getTrack(index < playlist.size() - 1 ? index + 1 : -1);
    }

    private Track nextShuffle() {
        //find non played minimum
        Track min = null;
        for (Track track : playlist) {
            if (track instanceof SeparatorTrack || track.isPlayed())
                continue;

            if (min == null || track.getShuffleRating() < min.getShuffleRating()) {
                min = track;
            }
        }
        if (min == null) {
            reshuffle();
            min = plMin;
        }

        return min;
    }

    private void reshuffle() {
        for (Track track : playlist) {
            track.setPlayed(false);
            track.setShuffleRating(Track.nextRandom());

            if (plMin == null || track.getShuffleRating() < plMin.getShuffleRating())
                plMin = track;

            if (plMax == null || track.getShuffleRating() > plMax.getShuffleRating())
                plMax = track;
        }
    }

    private Track getTrack(int index) {
        if (index != -1) {
            Track track = playlist.get(index);
            // technically, separator can not be the last track
            // so we just get the next track
            if (track instanceof SeparatorTrack)
                return playlist.get(index + 1);
            return track;
        } else {
            return null;
        }
    }

    public Track prev(Track currentTrack) {
        if (playlist == null || playlist.size() <= 0)
            return null;

        int index = playlist.indexOf(currentTrack);
        if (index == -1)
            return null;

        int size = playlist.size();

        switch (order) {
            case DEFAULT:
                index--;
                break;
            case REPEAT:
                index--;
                if (index < 0)
                    index += size;
                break;
            case REPEAT_TRACK:
                break;
            case REPEAT_ALBUM:
                String album = (String) albumFormat.eval(currentTrack);

                Track track = getTrack(index - 1);
                if (track != null) {
                    if (album.equals(albumFormat.eval(track))) {
                        return track;
                    }
                }

                for (int i = index; i < size; i++) {
                    track = playlist.get(i);
                    Object value = albumFormat.eval(track);
                    if (!album.equals(value)) {
                        return playlist.get(--i);
                    }
                }

                return track;
            case RANDOM:
                index = (int) (Math.random() * size);
                break;
            case SHUFFLE:
                return prevShuffle(currentTrack);
        }

        return getTrack(index);
    }

    private Track prevShuffle(Track currentTrack) {
        // find already played song with maximum shuffle rating below prev song
        Track max = null;
        Track amax = null;
        currentTrack.setPlayed(false);
        int rating = currentTrack.getShuffleRating();
        for (Track track : playlist) {
            if (track instanceof SeparatorTrack)
                continue;
            if (track != currentTrack && track.isPlayed() &&
                (amax == null || track.getShuffleRating() > amax.getShuffleRating())) {
                amax = track;
            }

            if (track == currentTrack || track.getShuffleRating() > rating || !track.isPlayed()) {
                continue;
            }

            if (max == null || track.getShuffleRating() > max.getShuffleRating()) {
                max = track;
            }
        }

        if (max == null) {
            if (amax == null) {
                reshuffle();
                amax = plMax;
            }

            max = amax;
        }
        return max;
    }
}