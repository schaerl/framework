/*
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.tests.components.media;

import com.vaadin.server.ClassResource;
import com.vaadin.server.Resource;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Audio;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;

public class AudioTest extends TestBase {

    @Override
    protected void setup() {

        // Public domain sounds from pdsounds.org 27.2.2013

        final Resource[] s1 = { new ClassResource(getClass(), "bip.mp3"),
                new ClassResource(getClass(), "bip.ogg") };
        final Resource[] s2 = {
                new ClassResource(getClass(), "toyphone_dialling.mp3"),
                new ClassResource(getClass(), "toyphone_dialling.ogg") };

        final Audio audio = new Audio();

        audio.setSources(s1);
        audio.setShowControls(true);
        audio.setHtmlContentAllowed(true);
        audio.setAltText("Can't <b>play</b> media");
        audio.setAutoplay(true);

        addComponent(audio);

        CheckBox checkBox = new CheckBox("Show controls");
        checkBox.setValue(audio.isShowControls());
        checkBox.addValueChangeListener(
                event -> audio.setShowControls(event.getValue()));
        addComponent(checkBox);
        checkBox = new CheckBox("HtmlContentAllowed");
        checkBox.setValue(audio.isHtmlContentAllowed());
        checkBox.addValueChangeListener(
                event -> audio.setHtmlContentAllowed(event.getValue()));
        addComponent(checkBox);
        checkBox = new CheckBox("muted");
        checkBox.setValue(audio.isMuted());
        checkBox.addValueChangeListener(
                event -> audio.setMuted(event.getValue()));
        addComponent(checkBox);
        checkBox = new CheckBox("autoplay");
        checkBox.setValue(audio.isAutoplay());
        checkBox.addValueChangeListener(
                event -> audio.setAutoplay(event.getValue()));
        addComponent(checkBox);

        Button b = new Button("Change", event -> audio.setSources(s2));
        addComponent(b);
        getLayout().setHeight("400px");
        getLayout().setExpandRatio(b, 1.0f);
    }

    @Override
    protected String getDescription() {
        return "Should autoplay, manipulating checkboxes should do appropriate thing, button changes file.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 11160;
    }

}
