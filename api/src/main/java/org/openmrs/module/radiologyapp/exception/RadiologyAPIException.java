package org.openmrs.module.radiologyapp.exception;

import org.openmrs.api.APIException;

public class RadiologyAPIException extends APIException {

    private static final long serialVersionUID = 1L;

    public RadiologyAPIException() {
        super();
    }

    public RadiologyAPIException(String message) {
        super(message);
    }

}
