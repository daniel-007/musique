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

package com.tulskiy.musique.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import org.jaudiotagger.tag.TagFieldKey;

import com.tulskiy.musique.playlist.Playlist;
import com.tulskiy.musique.playlist.Track;
import com.tulskiy.musique.util.Util;

/**
* Author: Denis Tulskiy
* Date: 11/12/10
*/
public abstract class SearchWorker extends SwingWorker<Playlist, Integer> {
    private final TagFieldKey[] fields = TagFieldKey.values();
    // TODO apply correct search metas, looking through all fields at the moment
//    {
//            "artist", "title", "album", "albumArtist", "fileName"
//    };
    private Playlist playlist;
    private String search;
    private boolean fillEmpty;
    protected ArrayList<Integer> viewToModelList = new ArrayList<Integer>();

    public SearchWorker(Playlist playlist, String search, boolean fillEmpty) {
        this.playlist = playlist;
        this.search = search;
        this.fillEmpty = fillEmpty;
    }

    @Override
    protected Playlist doInBackground() throws Exception {
        Playlist newPlaylist = new Playlist();

        String str = search.toLowerCase().trim();
        String[] text = str.split("\\s+");
        if (!str.isEmpty() && text.length > 0) {
            for (int i = 0, playlistSize = playlist.size(); i < playlistSize; i++) {
                Track track = playlist.get(i);

                boolean hasText[] = new boolean[text.length];
                for (TagFieldKey field : fields) {
                    List<String> values = track.getTrackData().getTagFieldValuesSafeAsList(field);
                    for (String value : values) {
	                    if (!Util.isEmpty(value)) {
	                        value = value.toLowerCase();
	                        String[] vals = value.split("\\s+");
	                        for (String val : vals) {
	                            for (int j = 0, textLength = text.length; j < textLength; j++) {
	                                String s = text[j];
	                                if (val.startsWith(s)) {
	                                    hasText[j] = true;
	                                }
	                            }
	                        }
	                    }
                    }
                }

                boolean toAdd = true;
                for (boolean b : hasText) {
                    toAdd &= b;
                }

                if (toAdd) {
                    newPlaylist.add(track);
                    viewToModelList.add(i);
                }
            }
        } else if (fillEmpty) {
            newPlaylist.addAll(playlist);
        }
        return newPlaylist;
    }

    @Override
    protected abstract void done();
}
