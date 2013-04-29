package org.openmrs.module.radiologyapp;

import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

import java.util.Date;


// can copy over other util methods from TestUtils in emr-api module
public class TestUtils {

    public static Matcher<Date> isJustNow() {
        return new ArgumentMatcher<Date>() {
            @Override
            public boolean matches(Object o) {
                // within the last second should be safe enough...
                return System.currentTimeMillis() - ((Date) o).getTime() < 1000;
            }
        };
    }

}
