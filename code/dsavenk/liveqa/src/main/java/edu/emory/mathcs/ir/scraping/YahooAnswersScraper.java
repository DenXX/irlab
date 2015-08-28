package edu.emory.mathcs.ir.scraping;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Extracts data from the Yahoo Answers QnA pages.
 */
public class YahooAnswersScraper {
    /**
     * Base url for question-answer pairs web pages. Only qid needs to be
     * appended.
     */
    public static final String BASE_URL =
            "https://answers.yahoo.com/question/index?qid=";

    /**
     * Base url for similar questions search page.
     */
    public static final String SEARCH_BASE_URL =
            "https://answers.yahoo.com/search/search_result?p=%s&s=%s";

    /**
     * Returns URL of a webpage for the question with the given id.
     * @param qid id of the question.
     * @return URL of the question.
     */
    public static String GetQuestionAnswerUrl(String qid) {
        return BASE_URL.concat(qid);
    }

    /**
     * Returns the URL for a related questions search page with the given query.
     * @param searchQuery The query to search for.
     * @param page
     * @return URL of the search page with the given query.
     */
    public static String getRelatedQuestionsSearchUrl(String searchQuery,
                                                      int page) {
        try {
            return String.format(SEARCH_BASE_URL, URLEncoder.encode(
                    searchQuery, "UTF-8"), Integer.toString(page));
        } catch (UnsupportedEncodingException e) {
            return String.format(SEARCH_BASE_URL,
                    URLEncoder.encode(searchQuery), Integer.toString(page));
        }
    }

    /**
     * Extracts question-answer pair information from the given Yahoo! Answers
     * page.
     * @param qnaUrl Url of the document to extract data from.
     * @return QuestionAnswer data extracted from the web page.
     */
    public static Optional<QuestionAnswer> GetQuestionAnswerData(URL qnaUrl) {
        try {
            QuestionAnswerExtractor extractor =
                    new QuestionAnswerExtractor();
            extractor.Init();
            WebPageScraper.scrape(qnaUrl,
                    new WebPageScraper.ProcessElementCallback[]
                            {extractor});
            return Optional.of(extractor.getQuestionAnswer());
        } catch (IOException e) {
            LiveQaLogger.LOGGER.warning(e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extracts question-answer pair information from a web page corresponding
     * to the given question id.
     * @param qid Id of the question of interest.
     * @return QuestionAnswer data extracted from the web page.
     */
    public static Optional<QuestionAnswer> GetQuestionAnswerData(String qid) {
        try {
            return GetQuestionAnswerData(new URL(GetQuestionAnswerUrl(qid)));
        } catch (MalformedURLException e) {
            LiveQaLogger.LOGGER.warning(e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the list of ids of questions related to the given query using
     * Yahoo! Answers search functionality.
     * @param searchQuery A query to submit to Yahoo! Answers.
     * @param similarQuestionsCount The number of similar questions to retrieve.
     * @return An array of string question identifiers.
     */
    public static String[] GetRelatedQuestionIds(String searchQuery,
                                                 int similarQuestionsCount) {
        YahooAnswersSearchResultsExtractor extractor =
                new YahooAnswersSearchResultsExtractor();
        extractor.Init();
        try {
            int lastIterQidsCount = -1;
            for (int page = 1;
                 extractor.length() < similarQuestionsCount &&
                         extractor.length() > lastIterQidsCount; ++page) {
                lastIterQidsCount = extractor.length();
                WebPageScraper.scrape(new URL(
                                getRelatedQuestionsSearchUrl(
                                        searchQuery, page)),
                        new WebPageScraper.ProcessElementCallback[]{extractor});
            }
            String[] res = extractor.GetQids();
            return extractor.length() > similarQuestionsCount ?
                    Arrays.copyOfRange(res, 0, similarQuestionsCount) :
                    res;
        } catch (IOException e) {
            LiveQaLogger.LOGGER.warning(e.getMessage());
            return new String[0];
        }
    }

    // Predicates that check if a particular DOM tree node is answer node,
    // title node, etc.
    private static boolean isAnswerNode(Element node) {
        return node.nodeName().equals("span") &&
                node.hasClass("ya-q-full-text");
    }

    private static boolean isQidNode(Element node) {
        return node.nodeName().equals("div") &&
                node.hasAttr("data-ya-question-id");
    }

    private static boolean isCategoryNode(Element node) {
        return node.nodeName().equals("a") &&
                node.className().contains("Clr-b") &&
                node.parent().id().equals("brdCrb");
    }

    private static boolean isAnswerBodyNode(Element node) {
        return node.nodeName().equals("meta") &&
                node.attr("name").equals("description");
    }

    private static boolean isQuestionTitleNode(Element node) {
        return node.nodeName().equals("meta") &&
                node.attr("name").equals("title");
    }

    /**
     * Stores information about a particular question-answer pair.
     */
    public static class QuestionAnswer {
        public String qid = "";
        public String title = "";
        public String body = "";
        public String[] categories = new String[0];
        public String bestAnswer = "";
        public String[] answers = new String[0];

        /**
         * Parses QnA pair from a string representation produced by
         * the toString method.
         *
         * @param line The line containing the QnA pair.
         * @return Parsed QuestionAnswer object.
         */
        public static QuestionAnswer parseFromString(String line) {
            QuestionAnswer res = new QuestionAnswer();
            String[] fields = line.split("\t", -1);
            res.qid = fields[0];
            res.title = fields[1];
            res.body = fields[2];
            res.bestAnswer = fields[3];
            res.categories = Arrays.stream(fields[4].split(","))
                    .filter(a -> a.length() > 0)
                    .toArray(String[]::new);
            res.answers = Arrays.stream(fields, 5, fields.length)
                    .filter(a -> a.length() > 0)
                    .toArray(String[]::new);
            return res;
        }

        private static String removeTabsAndNl(String str) {
            return str.replace("\t", "").replace("\n", " ").replace("\r", " ")
                    .replace(" +", " ");
        }

        @Override
        public String toString() {
            String cat = Arrays.stream(categories)
                    .map(QuestionAnswer::removeTabsAndNl)
                    .collect(Collectors.joining(","));
            String ans = Arrays.stream(answers)
                    .map(QuestionAnswer::removeTabsAndNl)
                    .collect(Collectors.joining("\t"));
            String mainPart = Arrays.stream(
                    new String[]{qid, title, body, bestAnswer})
                    .map(QuestionAnswer::removeTabsAndNl)
                    .collect(Collectors.joining("\t"));
            return String.format("%s\t%s\t%s", mainPart, cat, ans);
        }

        /**
         * @return Returns the question part of the QuestionAnswer.
         */
        public Question getQuestion() {
            return new Question(qid, title, body,
                    String.join("\t", categories));
        }

        /**
         * @return Returns the answer part of the given QuestionAnswer.
         */
        public Answer getAnswer() {
            return new Answer(bestAnswer,
                    YahooAnswersScraper.GetQuestionAnswerUrl(qid));
        }

    }

    /**
     * Object that implements a callback to extract question-answer pair data
     * fields from the Yahoo! Answers page.
     */
    public static class QuestionAnswerExtractor
            implements WebPageScraper.ProcessElementCallback {

        // Question-answer data that will be returned after the
        private QuestionAnswer result_ = new QuestionAnswer();
        private List<String> categories_ = new ArrayList<>();
        private List<String> answers_ = new ArrayList<>();

        /**
         * Init method needs to be called before the next document can be
         * processed.
         */
        public void Init() {
            result_ = new QuestionAnswer();
            categories_ = new ArrayList<>();
            answers_ = new ArrayList<>();
        }

        @Override
        public void processElement(Element e) {
            if (isQuestionTitleNode(e)) {
                result_.title = e.attr("content");
            } else if (isAnswerBodyNode(e)) {
                result_.body = e.attr("content");
            } else if (isCategoryNode(e)) {
                categories_.add(e.text());
            } else if (isQidNode(e)) {
                result_.qid = e.attr("data-ya-question-id");
            } else if (isAnswerNode(e)) {
                Element parent = e.parent();

                // Check parent to determine if this is the
                // best answer or just a regular answer.
                while (parent != null) {
                    if (parent.id().equals(
                            "ya-best-answer")) {
                        result_.bestAnswer = e.text();
                        break;
                    } else if (parent.id().equals(
                            "ya-qn-answers")) {
                        answers_.add(e.text());
                        break;
                    }
                    parent = parent.parent();
                }
            }
        }

        /**
         * @return The QuestionAnswer object with the data extracted from a web
         * page.
         */
        public QuestionAnswer getQuestionAnswer() {
            if (answers_ != null && answers_.size() > 0) {
                result_.answers = answers_.toArray(new String[answers_.size()]);
                answers_ = null;
            }
            if (categories_ != null && categories_.size() > 0) {
                result_.categories = categories_.toArray(
                        new String[categories_.size()]);
                categories_ = null;
            }
            return result_;
        }
    }

    /**
     * Object that implements a callback to extract list of similar questions
     * from Yahoo! Answers search results.
     */
    public static class YahooAnswersSearchResultsExtractor
            implements WebPageScraper.ProcessElementCallback {

        private List<String> qids_ = new ArrayList<>();

        /**
         * Initialized extractor. This method needs to be called before
         * processing new search results page.
         */
        void Init() {
            qids_ = new ArrayList<>();
        }

        @Override
        public void processElement(Element e) {
            if (e.nodeName().equals("li") &&
                    e.parent().id().equals("yan-questions")) {
                qids_.add(e.id().replace("q-", ""));
            }
        }

        /**
         * @return array of query ids extracted from Yahoo! Answers search
         * results page.
         */
        public String[] GetQids() {
            return qids_.toArray(new String[qids_.size()]);
        }

        /**
         * @return Returns the number of qids extracted.
         */
        public int length() {
            return qids_.size();
        }
    }
}