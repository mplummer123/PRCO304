package TwitterSentimentAnalysisFinal;

import com.mongodb.DBCollection;
import twitter4j.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Matthew Plummer.
 * This class will add a listener to Twitter to retrieve a feed of live feeds based user parameters selected.
 * This class operates in a similar fashion to the TwitterSearch class with regards to it calls to other classes.
 */

public class TwitterStream {
    public static String tweetText;
    public static Date tweetDate;
    static Object lock3 = new Object();
    static Object lock4 = new Object();
    static Object lock8 = new Object();
    static boolean analysisAndInsertCompleted = false;
    static List<Object> overallSentimentAndWordsFound = new ArrayList<>();
    static boolean duplicate = false;
    final static int[] currentNumberTweetsRetrieved = {0};



    public static void tweetStream(final DBCollection twitterColl, final List<String> sentimentWordDocumentList, final List<String> sentimentPolarityDocumentList, final int numberOfTweets, String wordsToSearch) {
        final int numberTweetsToRetrieve = numberOfTweets;
        String searchWords[] = {wordsToSearch};
        currentNumberTweetsRetrieved[0] = 0;

        final twitter4j.TwitterStream twitterStream = new TwitterStreamFactory().getInstance();

        StatusListener listener = new StatusListener() {

            public void onStatus(Status status) {

                //Check if it is a retweet, if so skip it
                if (!status.isRetweet()) {
                    if (currentNumberTweetsRetrieved[0] < numberTweetsToRetrieve) {
                        //Get information stated, this case is the date and text of tweet
                        tweetDate = status.getCreatedAt();
                        tweetText = status.getText();

                            synchronized (lock3) {
                                overallSentimentAndWordsFound = SentimentAnalysis.matchSentiment(sentimentPolarityDocumentList, sentimentWordDocumentList, tweetText);
                            }

                            synchronized (lock4)
                            {
                                analysisAndInsertCompleted = TweetAnalysisAndInsert.analysisAndInsert(overallSentimentAndWordsFound, tweetDate, tweetText, twitterColl);
                            }

                            if(analysisAndInsertCompleted == true) {
                                currentNumberTweetsRetrieved[0] += 1;
                                System.out.println("Number of Tweets retrieved: " + currentNumberTweetsRetrieved[0] + "/" + numberTweetsToRetrieve);
                            }
                            else
                            {
                                System.out.println("ERROR: The analysis and insert of the tweet was not completed successfully and return false or no return was given.");
                            }
                        }
                    }
                    if (currentNumberTweetsRetrieved[0] == numberTweetsToRetrieve) {
                        System.out.print("Number of specified tweets reached. Analysis complete.");
                        twitterStream.clearListeners();
                        twitterStream.shutdown();
                    }
                }

            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            }

            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            }

            @Override
            public void onScrubGeo(long l, long l1) {

            }

            @Override
            public void onStallWarning(StallWarning stallWarning) {

            }

            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };

        FilterQuery tweetFilterQuery = new FilterQuery();
        tweetFilterQuery.track(searchWords);//filter on words specified
        tweetFilterQuery.language(new String[]{"en"});//show only english tweets
        twitterStream.addListener(listener);
        twitterStream.filter(tweetFilterQuery);
    }
}
