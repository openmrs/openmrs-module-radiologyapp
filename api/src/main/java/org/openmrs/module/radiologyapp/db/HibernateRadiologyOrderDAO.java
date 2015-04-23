/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.radiologyapp.db;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.emrapi.db.HibernateSingleClassDAO;
import org.openmrs.module.radiologyapp.RadiologyOrder;

public class HibernateRadiologyOrderDAO extends HibernateSingleClassDAO<RadiologyOrder> implements RadiologyOrderDAO {

    public HibernateRadiologyOrderDAO() {
        super(RadiologyOrder.class);
    }

    @Override
    public RadiologyOrder getRadiologyOrderByOrderNumber(String orderNumber) {
        Criteria criteria = createRadiologyOrderCriteria();
        addOrderNumberRestriction(criteria, orderNumber);
        return (RadiologyOrder) criteria.uniqueResult();
    }


    private Criteria createRadiologyOrderCriteria() {
        return sessionFactory.getCurrentSession().createCriteria(RadiologyOrder.class);
    }

    private void addOrderNumberRestriction(Criteria criteria, String orderNumber) {
        criteria.add(Restrictions.eq("orderNumber", orderNumber));
    }
}

