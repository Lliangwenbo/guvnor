/*
 * Copyright 2011 JBoss Inc
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
package org.drools.guvnor.client.decisiontable.cells;

import java.util.List;

import org.drools.guvnor.client.decisiontable.widget.PatternsChangedEvent;
import org.drools.guvnor.client.messages.Constants;
import org.drools.ide.common.client.modeldriven.dt52.Pattern52;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

/**
 * A Popup drop-down Editor for bound Fact Patterns. This cell listens for
 * PatternsChangedEvents so that it's content can be maintained as and when
 * Patterns are added, deleted or edited.
 */
public class PopupBoundPatternDropDownEditCell extends
        AbstractPopupEditCell<String, String>
    implements
    PatternsChangedEvent.Handler {

    private Constants     constants = GWT.create( Constants.class );

    private final ListBox listBox;

    public PopupBoundPatternDropDownEditCell(EventBus eventBus) {
        super();
        this.listBox = new ListBox();

        // Tabbing out of the ListBox commits changes
        listBox.addKeyDownHandler( new KeyDownHandler() {

            public void onKeyDown(KeyDownEvent event) {
                boolean keyTab = event.getNativeKeyCode() == KeyCodes.KEY_TAB;
                boolean keyEnter = event.getNativeKeyCode() == KeyCodes.KEY_ENTER;
                if ( keyEnter || keyTab ) {
                    commit();
                }
            }

        } );

        vPanel.add( listBox );

        //Wire-up the events
        eventBus.addHandler( PatternsChangedEvent.TYPE,
                             this );
    }

    @Override
    public void render(Context context,
                       String value,
                       SafeHtmlBuilder sb) {
        if ( value != null ) {
            sb.append( renderer.render( value ) );
        }
    }

    public void onPatternsChanged(PatternsChangedEvent event) {
        setPatterns( event.getPatterns() );
    }

    /**
     * Set content of drop-down. All Patterns that are bound to a non-null,
     * non-empty name will be added to the ListBox
     * 
     * @param patterns
     */
    public void setPatterns(List<Pattern52> patterns) {
        listBox.clear();
        for ( Pattern52 p : patterns ) {
            String boundName = p.getBoundName();
            if ( !"".equals( boundName ) ) {
                listBox.addItem( boundName );
            }
        }
        listBox.setEnabled( listBox.getItemCount() > 0 );
        if ( listBox.getItemCount() == 0 ) {
            listBox.addItem( constants.NoPatternBindingsAvailable() );
        }
    }

    // Commit the change
    @Override
    protected void commit() {

        //If there are no pattern bindings don't update the model
        if ( !listBox.isEnabled() ) {
            return;
        }

        // Update value
        String value = null;
        int selectedIndex = listBox.getSelectedIndex();
        if ( selectedIndex >= 0 ) {
            value = listBox.getValue( selectedIndex );
        }
        setValue( lastContext,
                  lastParent,
                  value );
        if ( valueUpdater != null ) {
            valueUpdater.update( value );
        }
        panel.hide();

    }

    // Start editing the cell
    @Override
    protected void startEditing(final Context context,
                                final Element parent,
                                final String value) {

        // Select the appropriate item
        boolean emptyValue = (value == null);
        if ( emptyValue ) {
            listBox.setSelectedIndex( 0 );
        } else {
            for ( int i = 0; i < listBox.getItemCount(); i++ ) {
                if ( listBox.getValue( i ).equals( value ) ) {
                    listBox.setSelectedIndex( i );
                    break;
                }
            }
        }

        panel.setPopupPositionAndShow( new PositionCallback() {
            public void setPosition(int offsetWidth,
                                    int offsetHeight) {
                panel.setPopupPosition( parent.getAbsoluteLeft()
                                                + offsetX,
                                        parent.getAbsoluteTop()
                                                + offsetY );

                // Focus the first enabled control
                Scheduler.get().scheduleDeferred( new ScheduledCommand() {

                    public void execute() {
                        listBox.setFocus( true );
                    }

                } );
            }
        } );

    }

}
