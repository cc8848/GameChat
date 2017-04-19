package com.pajato.android.gamechat;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.pajato.android.gamechat.database.AccountManager;
import com.pajato.android.gamechat.main.MainActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.pajato.android.gamechat.main.MainActivity.TEST_USER_KEY;

/**
 * Provide a base class that includes setting up all tests to exclude the intro activity and perform
 * a do nothing test.
 *
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
@RunWith(AndroidJUnit4.class)
public abstract class BaseTest {

    // Private class constants.

    /** The test password key. */
    private static final String TEST_PASSWORD_KEY = "testPasswordKey";

    /** The test user provider. */
    private static final String TEST_PROVIDER_KEY = "testProviderKey";

    // Private class constants.

    /** The logcat tag. */
    private static final String TAG = BaseTest.class.getSimpleName();

    // Public instance variables.

    /** Set up the rule instance variable to allow for having intent extras passed in. */
    @Rule public ActivityTestRule<MainActivity> mRule =
            new ActivityTestRule<>(MainActivity.class, true, false);

    @Before public void setup() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.SKIP_INTRO_ACTIVITY_KEY, true);
        intent.putExtra(TEST_USER_KEY, getProperty("testAccountName", "nobody@gamechat.com"));
        intent.putExtra(TEST_PROVIDER_KEY, getProperty("testAccountProvider", "email"));
        intent.putExtra(TEST_PASSWORD_KEY, getProperty("testAccountPassword", null));
        mRule.launchActivity(intent);
    }

    @After public void teardown() {
        mRule.getActivity().finish();
    }

    /** Ensure that doing nothing breaks nothing but generates some code coverage results. */
    @Test public void testDoNothing() {
        // Do nothing initially.
    }

    // Protected methods.

    /** Setup the test user to run connected tests. */
    protected void setupTestUser(final Intent intent) {
        String login = intent.getStringExtra(TEST_USER_KEY);
        String pass = intent.getStringExtra(TEST_PASSWORD_KEY);
        if (login == null || pass == null) return;

        // Perform the sign in.
        FirebaseAuth.getInstance().signOut();
        AccountManager.instance.signIn(mRule.getActivity(), login, pass);
    }

    /** Provide access to our custom withDrawable matcher, that matches a view's drawable. */
    protected static Matcher<View> withDrawable(final int resourceId) {
        return new DrawableMatcher(resourceId);
    }

    /** Provide access to our custom noDrawable matcher. */
    protected static Matcher<View> noDrawable() {
        return new DrawableMatcher(-1);
    }

    // Private instance methods.

    /** Return a named system property or the given default value if there is no such property. */
    private String getProperty(final String propName, final String defaultValue) {
        String result = System.getProperty(propName);
        return result != null ? result : defaultValue;
    }

    // Private classes.

    /**
     * DrawableMatcher, a custom TypeSafeMatcher that facilitates the scanning of ImageViews to
     * determine their contents during espresso-related testing.
     *
     * @author Daniele Bottillo
     * @see "https://medium.com/@dbottillo/android-ui-test-espresso-matcher-for-imageview-1a28c832626f#.mnqccdvry"
     */
    private static class DrawableMatcher extends TypeSafeMatcher<View> {

        private final int expectedId;
        private String resourceName;

        DrawableMatcher(final int expectedId) {
            super(View.class);
            this.expectedId = expectedId;
        }

        @Override protected boolean matchesSafely(final View target) {
            if (!(target instanceof ImageView)){
                return false;
            }
            ImageView imageView = (ImageView) target;
            if (expectedId < 0){
                return imageView.getDrawable() == null;
            }
            Resources resources = target.getContext().getResources();
            Drawable expectedDrawable = ResourcesCompat.getDrawable(resources, expectedId, null);
            resourceName = resources.getResourceEntryName(expectedId);

            if (expectedDrawable == null) {
                return false;
            }

            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            Bitmap otherBitmap = ((BitmapDrawable) expectedDrawable).getBitmap();
            return bitmap.sameAs(otherBitmap);
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("with drawable from resource id: ");
            description.appendValue(expectedId);
            if (resourceName != null) {
                description.appendText("[");
                description.appendText(resourceName);
                description.appendText("]");
            }
        }
    }

}
