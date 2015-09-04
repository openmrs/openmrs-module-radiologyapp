package org.openmrs.module.radiologyapp.comparator;


import org.openmrs.Order;
import org.openmrs.module.radiologyapp.RadiologyStudy;

import java.util.Comparator;

public class RadiologyOrderByDateComparator implements Comparator<Order>{
    @Override
    public int compare(Order order1, Order order2) {

        if (order1 == null || order1.getDateCreated() == null) {
            return 1;
        }

        if (order2 == null || order2.getDateCreated() == null)  {
            return -1;
        }

        // note that we are sorting so that most recent date is first
        return order2.getDateCreated().compareTo(order1.getDateCreated());
    }
}
