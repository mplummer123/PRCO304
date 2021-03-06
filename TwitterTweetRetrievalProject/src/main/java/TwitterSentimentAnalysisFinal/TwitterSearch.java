package TwitterSentimentAnalysisFinal;

import com.mongodb.DBCollection;
import twitter4j.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Matthew Plummer.
 * This class executes code to carry out its purpose of a Twitter search.
 * This class is only run if the user selects the option to carry out a search from the UI.
 * Other classes are called for this class in a similar fashion as that in the TwitterStream class.
 * A clause to keep record of the API limitations with regards to the number of requests are stored here.
 */
public class TwitterSearch {
    static Object lock5 = new Object();
    static Object lock6 = new Object();
    static List<Object> overallSentimentAndWordsFound = new ArrayList<>();
    static boolean analysisAndInsertCompleted = false;
    static boolean searchCompleted = false;
    static String tweetText;
    static Date tweetDate;
    public static int requests = 0;


    public static boolean searchTwitter(DBCollection twitterColl, final List<String> sentimentWordDocumentList, final List<String> sentimentPolarityDocumentList, String wordsToSearch, String searchDate) {
        Twitter twitter = new TwitterFactory().getInstance();
        Query query = new Query(wordsToSearch);
        query.setUntil(searchDate);
        query.setLang("en");
        QueryResult result;
        int Count=0;

        if(requests >= 170)
        {
            return searchCompleted;//Stop searching as request limit has been met
        }

        try {
            do {
                result = twitter.search(query);
                requests ++;
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {

                    tweetText = tweet.getText();
                    tweetDate = tweet.getCreatedAt();

                    synchronized (lock5) {
                        overallSentimentAndWordsFound = SentimentAnalysis.matchSentiment(sentimentPolarityDocumentList, sentimentWordDocumentList, tweetText);
                    }

                    synchronized (lock6)
                    {
                        analysisAndInsertCompleted = TweetAnalysisAndInsert.analysisAndInsert(overallSentimentAndWordsFound, tweetDate, tweetText, twitterColl);
                    }
                    Count++;
                    System.out.println("Number of tweets retrieved currently: " + Count);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //This keeps the code iterating as long as there is another Tweet
            //the request clause is needed to ensure that the request limit is not exceeded causing the code to be locked out.
            while ((query = result.nextQuery()) != null && requests <= 170);

            System.out.println("NUMBER OF TWEETS = " + Count);
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        searchCompleted = true;
        return searchCompleted;
    }
}
