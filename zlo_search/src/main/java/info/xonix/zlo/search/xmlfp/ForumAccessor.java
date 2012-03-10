package info.xonix.zlo.search.xmlfp;

import info.xonix.zlo.search.model.Message;
import info.xonix.zlo.search.xmlfp.jaxb_generated.Forum;
import info.xonix.zlo.search.xmlfp.utils.MarshalUtils;
import info.xonix.zlo.search.xmlfp.utils.UrlUtil;
import info.xonix.zlo.search.xmlfp.utils.XmlFpMarshalException;
import org.apache.commons.io.IOUtils;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

/**
 * User: gubarkov
 * Date: 12.02.12
 * Time: 0:33
 */
public class ForumAccessor {
    private Forum forum;

    private ForumAccessor() {
    }

    public static ForumAccessor fromDescriptorUrl(String descriptorUrl) throws XmlFpException {
        ForumAccessor forumAccessor = new ForumAccessor();

        final byte[] descriptorXmlBytes = getXmlAsBytesFromUrl(descriptorUrl);

        try {
            forumAccessor.forum = (Forum) XmlFpContext.getUnmarshaller().unmarshal(new ByteArrayInputStream(descriptorXmlBytes));
        } catch (JAXBException e) {
            throw new XmlFpException(e, "Error unmarshalling");
        }

        forumAccessor.absolutizeUrls(descriptorUrl);
        return forumAccessor;
    }

    public static ForumAccessor fromDescriptorXmlString(String descriptorUrl, String descriptorXml) throws XmlFpException {
        ForumAccessor forumAccessor = new ForumAccessor();

        try {
            forumAccessor.forum = (Forum) XmlFpContext.getUnmarshaller().unmarshal(new StringReader(descriptorXml));
        } catch (JAXBException e) {
            throw new XmlFpException(e, "Error unmarshalling");
        }

        forumAccessor.absolutizeUrls(descriptorUrl);
        return forumAccessor;
    }

    private void absolutizeUrls(String descriptorUrl) {
        final Forum.XmlfpUrls xmlfpUrls = forum.getXmlfpUrls();

        xmlfpUrls.setLastMessageNumberUrl(UrlUtil.combineUrls(descriptorUrl, xmlfpUrls.getLastMessageNumberUrl()));
        xmlfpUrls.setMessageUrl(UrlUtil.combineUrls(descriptorUrl, xmlfpUrls.getMessageUrl()));
        xmlfpUrls.setMessageListUrl(UrlUtil.combineUrls(descriptorUrl, xmlfpUrls.getMessageListUrl()));

        final Forum.ForumUrls forumUrls = forum.getForumUrls();
        forumUrls.setMessageUrl(UrlUtil.combineUrls(descriptorUrl, forumUrls.getMessageUrl()));
        forumUrls.setUserProfileUrl(UrlUtil.combineUrls(descriptorUrl, forumUrls.getUserProfileUrl()));
    }

    public String formDescriptorXml() throws XmlFpMarshalException {
        return MarshalUtils.marshal(XmlFpContext.getMarshaller(), forum);
    }

    public long getLastMessageNumber() throws XmlFpException {
        final byte[] bytes = getXmlAsBytesFromUrl(forum.getXmlfpUrls().getLastMessageNumberUrl());

        try {
            @SuppressWarnings("unchecked")
            final JAXBElement<Long> res = (JAXBElement<Long>) XmlFpContext.getUnmarshaller()
                    .unmarshal(new ByteArrayInputStream(bytes));

            return res.getValue();
        } catch (JAXBException e) {
            throw new XmlFpException(e, "Error unmarshalling");
        }
    }

    public Message getMessage(long id) throws XmlFpException {
        final String messageUrl = forum.getXmlfpUrls().getMessageUrl();
        final String messageUrlFilled = messageUrl.replace(
                XmlFpUrlsSubstitutions.MESSAGE_ID, Long.toString(id));

        final byte[] bytes = getXmlAsBytesFromUrl(messageUrlFilled);

        try {
            @SuppressWarnings("unchecked")
            final info.xonix.zlo.search.xmlfp.jaxb_generated.Message jaxbMessage = (info.xonix.zlo.search.xmlfp.jaxb_generated.Message) XmlFpContext.getUnmarshaller()
                    .unmarshal(new ByteArrayInputStream(bytes));

            return Convert.fromJaxbMessage(jaxbMessage);
        } catch (JAXBException e) {
            throw new XmlFpException(e, "Error unmarshalling");
        }

    }

    //TODO: implement retry for download
    private static byte[] getXmlAsBytesFromUrl(final String url) throws XmlFpException {
        final byte[] bytes;

        try {
            bytes = IOUtils.toByteArray(new URL(url).openStream());
        } catch (IOException e) {
            throw new XmlFpIOException(e, "Error accessing forum site");
        }

        return bytes;
    }

    public String getForumMessageUrl(long messageId) {
        return forum.getForumUrls().getMessageUrl()
                .replace(XmlFpUrlsSubstitutions.MESSAGE_ID, Long.toString(messageId));
    }

    public String getForumUserProfileUrl(long userId, String userName) {
        return forum.getForumUrls().getUserProfileUrl()
                .replace(XmlFpUrlsSubstitutions.USER_ID, Long.toString(userId))
                .replace(XmlFpUrlsSubstitutions.USER_NAME, userName);
    }

    public String getForumUrl() {
        return forum.getUrl();
    }

    public String getDescription() {
        return forum.getDescription();
    }

    public String getTitle() {
        return forum.getName();
    }
}