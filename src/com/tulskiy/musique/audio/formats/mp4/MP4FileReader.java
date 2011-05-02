/*
 * Copyright (c) 2008, 2009, 2010, 2011 Denis Tulskiy
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

package com.tulskiy.musique.audio.formats.mp4;

import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.mp4.Mp4FileReader;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.mp4.Mp4Tag;

import com.tulskiy.musique.audio.AudioFileReader;
import com.tulskiy.musique.playlist.Track;

/**
 * @Author: Denis Tulskiy
 * @Date: 11.08.2009
 */
public class MP4FileReader extends AudioFileReader {
    @Override
    public Track readSingle(Track track) {
        Mp4FileReader reader = new Mp4FileReader();
        try {
            org.jaudiotagger.audio.AudioFile audioFile = reader.read(track.getTrackData().getFile());
            copyHeaderFields((GenericAudioHeader) audioFile.getAudioHeader(), track);
            org.jaudiotagger.tag.Tag tag = audioFile.getTag();
            copyCommonTagFields(tag, track);
            copySpecificTagFields(tag, track);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't read file: " + track.getTrackData().getFile());
        }

        return track;
    }

    @Override
    public boolean isFileSupported(String ext) {
        return (ext.equalsIgnoreCase("mp4") || ext.equalsIgnoreCase("m4a"));
    }

    @Override
    protected void copySpecificTagFields(Tag tag, Track track) {
    	Mp4Tag mp4Tag = (Mp4Tag) tag;
        // TODO implement when trunk jaudiotagger come
//      track.setTrackNumber(tag.getFirstTrack());
//      track.setDiscNumber(tag.getFirst("disk"));
//      track.setMeta("albumArtist", tag.getFirst("aART"));
    }

}
