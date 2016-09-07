/*******************************************************************************
 ******************************************************************************/
package org.apache.taverna.ui.perspectives.myexperiment.model;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;

/**
 * @author Sergejs Aleksejevs
 */
public class Comment extends Resource {
  private User user;
  private String comment;
  private int typeOfCommentedResource;
  private String uriOfCommetedResource;

  public Comment() {
	super();
	this.setItemType(Resource.COMMENT);
  }

  public User getUser() {
	return (this.user);
  }

  public void setUser(User user) {
	this.user = user;
  }

  public String getComment() {
	return (this.comment);
  }

  public void setComment(String comment) {
	this.comment = comment;
  }

  public int getTypeOfCommentedResource() {
	return (this.typeOfCommentedResource);
  }

  public void setTypeOfCommentedResource(int typeOfCommentedResource) {
	this.typeOfCommentedResource = typeOfCommentedResource;
  }

  public String getURIOfCommentedResource() {
	return (this.uriOfCommetedResource);
  }

  public void setURIOfCommentedResource(String uriOfCommetedResource) {
	this.uriOfCommetedResource = uriOfCommetedResource;
  }

  /**
   * A helper method to return a set of API elements that are needed to satisfy
   * request of a particular type - e.g. creating a listing of resources or
   * populating full preview, etc.
   * 
   * @param iRequestType
   *          A constant value from Resource class.
   * @return Comma-separated string containing values of required API elements.
   */
  public static String getRequiredAPIElements(int iRequestType) {
	String strElements = "";

	// cases higher up in the list are supersets of those that come below -
	// hence no "break" statements are required, because 'falling through' the
	// switch statement is the desired behaviour in this case
	switch (iRequestType) {
	  case Resource.REQUEST_DEFAULT_FROM_API:
		strElements += ""; // no change needed - defaults will be used
	}

	return (strElements);
  }

  //class method to build a comment instance from XML
  public static Comment buildFromXML(Document doc, Logger logger) {
	// if no XML was supplied, return null to indicate an error
	if (doc == null)
	  return (null);

	Comment c = new Comment();

	try {
	  Element root = doc.getRootElement();

	  c.setResource(root.getAttributeValue("resource"));
	  c.setURI(root.getAttributeValue("uri"));

	  c.setTitle(root.getChildText("comment"));
	  c.setComment(root.getChildText("comment"));

	  Element commentedResourceElement = root.getChild("subject");
	  if (commentedResourceElement != null) {
		c.setTypeOfCommentedResource(Resource.getResourceTypeFromVisibleName(commentedResourceElement.getName()));
		c.setURIOfCommentedResource(commentedResourceElement.getAttributeValue("uri"));
	  }

	  Element userElement = root.getChild("author");
	  c.setUser(Util.instantiatePrimitiveUserFromElement(userElement));

	  String createdAt = root.getChildText("created-at");
	  if (createdAt != null && !createdAt.equals("")) {
		c.setCreatedAt(MyExperimentClient.parseDate(createdAt));
	  }

	  logger.debug("Found information for comment with ID: " + c.getID()
		  + ", URI: " + c.getURI());
	} catch (Exception e) {
	  logger.error("Failed midway through creating comment object from XML", e);
	}

	// return created comment instance
	return (c);
  }

}
