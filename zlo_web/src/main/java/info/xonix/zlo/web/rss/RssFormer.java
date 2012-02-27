package info.xonix.zlo.web.rss;

import de.nava.informa.core.CategoryIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.impl.basic.Category;
import de.nava.informa.impl.basic.Channel;
import de.nava.informa.impl.basic.Item;
import info.xonix.zlo.search.HttpHeader;
import info.xonix.zlo.search.ZloPaginatedList;
import info.xonix.zlo.search.config.Config;
import info.xonix.zlo.search.domainobj.SearchRequest;
import info.xonix.zlo.search.domainobj.SearchResult;
import info.xonix.zlo.search.domainobj.Site;
import info.xonix.zlo.search.model.Message;
import info.xonix.zlo.search.spring.AppSpringContext;
import info.xonix.zlo.search.utils.HtmlUtils;
import info.xonix.zlo.web.servlets.SearchServlet;
import info.xonix.zlo.web.servlets.helpful.ForwardingRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * User: Vovan
 * Date: 15.08.2009
 * Time: 20:53:36
 */
public class RssFormer {
    private static final Logger log = Logger.getLogger(RssFormer.class);

    private static final Config config = AppSpringContext.get(Config.class);

    public void formRss(ForwardingRequest request, HttpServletResponse response) {
        if (request.getAttribute(SearchServlet.ERROR) != null) {
            // process error
        } else {
            response.setContentType("application/rss+xml");
            response.setCharacterEncoding("windows-1251");

            SearchResult searchResult = (SearchResult) request.getAttribute(SearchServlet.REQ_SEARCH_RESULT);
            Assert.notNull(searchResult, "SearchResult must be already set in SearchServlet!");

            ZloPaginatedList pl = (ZloPaginatedList) request.getAttribute(SearchServlet.REQ_PAGINATED_LIST);
            Assert.notNull(pl, "Paginated list must be already set in SearchServlet!");

            List msgsList = pl.getList();
            Date lastModifiedDateCurrent = msgsList != null && msgsList.size() > 0 ? ((Message) msgsList.get(0)).getDate() : null; // the youngest msg (max date)

            log.info("RSS request. User-Agent: " + request.getHeader(HttpHeader.USER_AGENT) + ", If-Modified-Since: " + request.getHeader(HttpHeader.IF_MODIFIED_SINCE));

            if (lastModifiedDateCurrent != null) {
                Date lastModifiedDateOld = new Date(request.getDateHeader("If-Modified-Since"));

                if (lastModifiedDateCurrent.after(lastModifiedDateOld)) {
                    response.setDateHeader("Last-Modified", lastModifiedDateCurrent.getTime());
                    log.info("Feed is modified: If-Modified-Since Date=" + lastModifiedDateOld + ", lastMsgFeedDate=" + lastModifiedDateCurrent);
                } else {
                    log.info("Sending 304 Not modified: If-Modified-Since Date=" + lastModifiedDateOld + " is _not before_ lastMsgFeedDate=" + lastModifiedDateCurrent);
                    response.setStatus(304); // Not Modified
                    return;
                }
            }

            SearchRequest lastSearch = searchResult.getLastSearch();

            ChannelIF ch = new Channel();

            List<String> l = new ArrayList<String>(3);
            for (String s : Arrays.asList(lastSearch.getText(), lastSearch.getNick(), lastSearch.getHost())) {
                if (StringUtils.isNotEmpty(s))
                    l.add(s);
            }
            String chTitle = "Board search: " + StringUtils.join(l, ", ");

            ch.setTitle(chTitle);

            try {
                ch.setLocation(new URL(String.format("http://%s/search?%s", config.getWebsiteDomain(), request.getQueryString().replace("rss&", ""))));
                ch.setDescription(lastSearch.describeToString());
                ch.setLanguage("ru");
                ch.setTtl(120); // 2 hours

//                FoundTextHighlighter hl = new FoundTextHighlighter();
//                hl.setHighlightWords(FoundTextHighlighter.formHighlightedWords(lastSearch.getText()));

                if (msgsList != null) {
                    for (Object m1 : msgsList) {
                        Message m = (Message) m1;
                        Item it = new Item();

//                        Site s = m.getSite();
                        Site s = pl.getSite();

                        // highlighting of all feed takes to many time & cpu

                        String title = HtmlUtils.unescapeHtml(m.getTitle());
                        if (!"без темы".equals(m.getTopic().toLowerCase()))
                            title = "[" + m.getTopic() + "] " + title;
                        it.setTitle(title);
                        it.setDescription(m.getBody());
                        it.setCreator(m.getNick() + "@" + m.getHost());
                        it.setDate(m.getDate());
                        it.setCategories(Arrays.asList((CategoryIF) new Category(m.getTopic())));

                        // let it point to forum msg, not saved msg
//                        it.setLink(new URL(String.format("http://%s/msg?site=%s&num=%s&hw=%s", Config.WEBSITE_DOMAIN, s.getNum(), m.getNum(), HtmlUtils.urlencode(hl.getWordsStr()))));
                        URL commentsUrl = new URL(String.format("http://%s%s%s", s.getSiteUrl(), s.getReadQuery(), m.getNum()));
                        it.setLink(commentsUrl);
                        it.setComments(commentsUrl);

                        ch.addItem(it);
                    }
                }

                ZloRss20Exporter exporter = new ZloRss20Exporter(response.getWriter(), "windows-1251");

                exporter.write(ch);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
