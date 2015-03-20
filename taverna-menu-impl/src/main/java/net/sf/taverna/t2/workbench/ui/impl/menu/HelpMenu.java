/*******************************************************************************
 * Copyright (C) 2007 The University of Manchester   
 * 
 *  Modifications to the initial code base are copyright of their
 *  respective authors, or their employers as appropriate.
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *    
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *    
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 ******************************************************************************/
package net.sf.taverna.t2.workbench.ui.impl.menu;

import static java.awt.event.KeyEvent.VK_H;
import static javax.swing.Action.MNEMONIC_KEY;
import static net.sf.taverna.t2.ui.menu.DefaultMenuBar.DEFAULT_MENU_BAR;

import java.net.URI;

import net.sf.taverna.t2.ui.menu.AbstractMenu;

public class HelpMenu extends AbstractMenu {
	public static final URI HELP_URI = URI
			.create("http://taverna.sf.net/2008/t2workbench/menu#help");

	public HelpMenu() {
		super(DEFAULT_MENU_BAR, 1024, HELP_URI, makeAction());
	}

	public static DummyAction makeAction() {
		DummyAction action = new DummyAction("Help");
		action.putValue(MNEMONIC_KEY, Integer.valueOf(VK_H));
		return action;
	}
}