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
package net.sf.taverna.t2.renderers.impl;

import java.awt.Font;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import net.sf.taverna.t2.lang.ui.DialogTextArea;
import net.sf.taverna.t2.renderers.Renderer;
import net.sf.taverna.t2.renderers.RendererException;
import net.sf.taverna.t2.renderers.RendererUtils;

import org.apache.log4j.Logger;

import uk.org.taverna.databundle.DataBundles;

/**
 * Renderer for mime type text/*
 *
 * @author Ian Dunlop
 * @author Alex Nenadic
 * @author David Withers
 */
public class TextRenderer implements Renderer {

	private Pattern pattern;

	private int MEGABYTE = 1024 * 1024;

	private Logger logger = Logger.getLogger(TextRenderer.class);

	public TextRenderer() {
		pattern = Pattern.compile(".*text/.*");
	}

	@Override
	public boolean canHandle(String mimeType) {
		return pattern.matcher(mimeType).matches();
	}

	@Override
	public String getType() {
		return "Text";
	}

	@Override
	public JComponent getComponent(Path path) throws RendererException {
		if (DataBundles.isValue(path) || DataBundles.isReference(path)) {
			try {
				DialogTextArea theTextArea = new DialogTextArea();
				theTextArea.setWrapStyleWord(true);
				theTextArea.setEditable(false);
				long approximateSizeInBytes = 0;
				try {
					approximateSizeInBytes = RendererUtils.getSizeInBytes(path);
				} catch (Exception ex) {
					logger.error("Failed to get the size of the data", ex);
					return new JTextArea(
							"Failed to get the size of the data (see error log for more details): \n"
									+ ex.getMessage());
				}

				if (approximateSizeInBytes > MEGABYTE) {
					Object[] options = { "Continue rendering", "Render partial", "Cancel" };
					// allow partial rendering of text files
					int response = JOptionPane
							.showOptionDialog(
									null,
									"Result is approximately "
											+ bytesToMeg(approximateSizeInBytes)
											+ " MB in size, there could be issues with rendering this inside Taverna\nDo you want to cancel, render all of the result, or only the first part?",
									"Rendering large result", JOptionPane.YES_NO_CANCEL_OPTION,
									JOptionPane.QUESTION_MESSAGE, null, options, options[2]);

					if (response == JOptionPane.YES_OPTION) {
						// User still wants to render the big data
						String resolve = null;
						try {
							// Resolve it as a string
							resolve = RendererUtils.getString(path);
						} catch (Exception e) {
							logger.error("Reference Service failed to render data as string", e);
							return new JTextArea(
									"Reference Service failed to render data as string (see error log for more details): \n"
											+ e.getMessage());
						}

						theTextArea.setText(resolve);
						theTextArea.setCaretPosition(0);
						theTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
						return theTextArea;

					} else if (response == JOptionPane.NO_OPTION) {
						byte[] smallStringBytes = new byte[1048576];
						try (InputStream inputStream = RendererUtils.getInputStream(path)) {
							// just copy the first MEGABYTE
							inputStream.read(smallStringBytes);
						} catch (Exception ex) {
							logger.error("Reference Service failed to render data as byte array",
									ex);
							return new JTextArea(
									"Reference Service failed to render data as byte array (see error log for more details): \n"
											+ ex.getMessage());
						}
						theTextArea.setText(new String(smallStringBytes));
						theTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
						theTextArea.setCaretPosition(0);
						return theTextArea;
					} else {
						// if (response == JOptionPane.CANCEL_OPTION) or ESCAPE key pressed
						theTextArea
								.setText(new String(
										"Rendering cancelled due to size of data. Try saving and viewing in an external application."));
						theTextArea.setCaretPosition(0);
						return theTextArea;
					}
				} else { // Data is not too big
					String resolve = null;
					try {
						// Resolve it as a string
						resolve = RendererUtils.getString(path);
					} catch (Exception e) {
						logger.error("Reference Service failed to render data as string", e);
						return new JTextArea(
								"Reference Service failed to render data as string (see error log for more details): \n"
										+ e.getMessage());
					}
					theTextArea.setText(resolve);
					theTextArea.setCaretPosition(0);
					theTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
					return theTextArea;
				}
			} catch (Exception e1) {
				logger.error("Failed to create text renderer", e1);
				return new JTextArea(
						"Failed to create text renderer (see error log for more details): \n"
								+ e1.getMessage());
			}
		} else {
			logger.error("Failed to obtain the data to render: data is not a value or reference");
			return new JTextArea(
					"Failed to obtain the data to render: data is not a value or reference");
		}
	}

	/**
	 * Work out size of file in megabytes to 1 decimal place
	 *
	 * @param bytes
	 * @return
	 */
	private int bytesToMeg(long bytes) {
		float f = bytes / MEGABYTE;
		return Math.round(f);
	}

}
