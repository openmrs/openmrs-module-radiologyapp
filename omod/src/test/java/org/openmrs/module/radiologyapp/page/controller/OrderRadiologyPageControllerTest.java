package org.openmrs.module.radiologyapp.page.controller;

import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Visit;
import org.openmrs.api.LocationService;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderRadiologyPageControllerTest {

    private static final String TAG_NAME = "Order Radiology Study Location";

    private OrderRadiologyPageController controller = new OrderRadiologyPageController();

    private Location newLocation(String name) {
        Location location = new Location();
        location.setName(name);
        return location;
    }

    // ----- collectTaggedLocations -----

    @Test
    public void collectTaggedLocations_shouldIncludeRootLocationIfTagged() {
        LocationTag tag = new LocationTag(TAG_NAME, "");
        Location root = newLocation("Root");
        root.addTag(tag);

        List<Location> collected = new ArrayList<Location>();
        controller.collectTaggedLocations(root, tag, collected);

        assertThat(collected, containsInAnyOrder(root));
    }

    @Test
    public void collectTaggedLocations_shouldIncludeTaggedDescendant() {
        LocationTag tag = new LocationTag(TAG_NAME, "");
        Location root = newLocation("Root");
        Location child = newLocation("Child");
        Location grandchild = newLocation("Grandchild");
        root.addChildLocation(child);
        child.addChildLocation(grandchild);
        grandchild.addTag(tag);

        List<Location> collected = new ArrayList<Location>();
        controller.collectTaggedLocations(root, tag, collected);

        assertThat(collected, containsInAnyOrder(grandchild));
    }

    @Test
    public void collectTaggedLocations_shouldNotIncludeTaggedLocationOutsideSubtree() {
        LocationTag tag = new LocationTag(TAG_NAME, "");
        Location grandparent = newLocation("Grandparent");
        Location root = newLocation("Root");
        Location sibling = newLocation("Sibling");
        grandparent.addChildLocation(root);
        grandparent.addChildLocation(sibling);
        sibling.addTag(tag); // tagged, but not under root

        List<Location> collected = new ArrayList<Location>();
        controller.collectTaggedLocations(root, tag, collected);

        assertThat(collected, empty());
    }

    @Test
    public void collectTaggedLocations_shouldNotIncludeUntaggedLocationInSubtree() {
        LocationTag tag = new LocationTag(TAG_NAME, "");
        Location root = newLocation("Root");
        Location child = newLocation("Child"); // in subtree, but not tagged
        root.addChildLocation(child);

        List<Location> collected = new ArrayList<Location>();
        controller.collectTaggedLocations(root, tag, collected);

        assertThat(collected, empty());
    }

    // ----- getOrderRadiologyStudyLocations -----

    @Test
    public void getOrderRadiologyStudyLocations_shouldReturnTaggedLocationsUnderVisitLocation() {
        LocationTag tag = new LocationTag(TAG_NAME, "");

        Location visitLocation = newLocation("Visit Location");
        visitLocation.setLocationId(1);
        visitLocation.addTag(tag);

        Location taggedChild = newLocation("Tagged Child");
        taggedChild.setLocationId(2);
        taggedChild.addTag(tag);
        visitLocation.addChildLocation(taggedChild);

        Location untaggedChild = newLocation("Untagged Child");
        untaggedChild.setLocationId(3);
        visitLocation.addChildLocation(untaggedChild);

        Visit visit = new Visit();
        visit.setLocation(visitLocation);

        LocationService locationService = mock(LocationService.class);
        when(locationService.getLocationTagByName(TAG_NAME)).thenReturn(tag);

        UiUtils ui = mock(UiUtils.class);
        when(ui.format(any())).thenAnswer(invocation -> ((Location) invocation.getArgument(0)).getName());

        List<SimpleObject> result = controller.getOrderRadiologyStudyLocations(locationService, visit, ui);

        List<Object> ids = new ArrayList<Object>();
        for (SimpleObject item : result) {
            ids.add(item.get("value"));
        }
        assertThat(ids, containsInAnyOrder((Object) 1, (Object) 2));
    }

    @Test
    public void getOrderRadiologyStudyLocations_shouldReturnEmptyListAndSkipTraversalIfTagDoesNotExist() {
        Visit visit = mock(Visit.class);

        LocationService locationService = mock(LocationService.class);
        when(locationService.getLocationTagByName(TAG_NAME)).thenReturn(null);

        UiUtils ui = mock(UiUtils.class);

        List<SimpleObject> result = controller.getOrderRadiologyStudyLocations(locationService, visit, ui);

        assertThat(result, empty());
        verify(visit, never()).getLocation();
    }
}
