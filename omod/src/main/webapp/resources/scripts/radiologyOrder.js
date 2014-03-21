function AutocompleteItem(id, name) {
    var api = {};
    api.id = id;
    api.name = name;

    return api;
}

// TODOS:
// creatinine level must be a number
// make the dialog box appear if creatinine is over 1.5
// handle customizing the error message
// handle getting the orderables into the view and setting the global properties
// handle submitting and saving the values

function StudiesViewModel(studies, locations, requiredFields, contrastStudies) {

    // private variables

    // array of concept id of studies that require contrast
    var contrastStudies = contrastStudies;

    // api
    var api = {};
    api.searchTerm = ko.observable(true);
    api.selectedStudies = ko.observableArray([]);
    api.studies = ko.observableArray([]);

    api.portable = ko.observable(false);
    api.locations = ko.observableArray([]);
    api.portableLocation = ko.observable();
    api.clinicalHistory = ko.observable();
    api.requestedBy = ko.observable();
    api.requestedFrom = ko.observable();
    api.requestedOn = ko.observable();

    api.creatinineLevel = ko.observable();
    api.creatinineTestDate = ko.observable();

    api.requiredFields = requiredFields;

    api.isValid = function() {

        var studiesAreValid = api.selectedStudies().length > 0;
        var portableIsValid = api.portable() ? api.portableLocation() != null : true;

        var clinicalHistoryValid =  api.requiredFields.indexOf('clinicalHistory') == -1   // always valid if not specified in the required array
            ||  api.clinicalHistory() != null && (api.clinicalHistory().match(/\w+/) != null);

        var requestedByValid = api.requiredFields.indexOf('requestedBy') == -1   // always valid if not specified in the required array
            || (api.requestedBy() != null && (api.requestedBy().match(/\w+/) != null));

        var requestedFromValid = api.requiredFields.indexOf('requestedFrom') == -1   // always valid if not specified in the required array
            || (api.requestedFrom() != null && (api.requestedFrom().match(/\w+/) != null));

        var requestedOnValid = api.requiredFields.indexOf('requestedOn') == -1   // always valid if not specified in the required array
            || (api.requestedOn() != null && (api.requestedOn ().match(/\w+/) != null));

        // creatinine fields are mandatory if a constrast study has been requested
        var creatinineFieldsValid = !api.selectedStudiesIncludeContrastStudy()
            || ((api.creatinineLevel() != null && (api.creatinineLevel().match(/\w+/) != null))
            && (api.creatinineTestDate() != null && (api.creatinineTestDate ().match(/\w+/) != null)));

        return studiesAreValid && portableIsValid && clinicalHistoryValid && requestedByValid
            && requestedFromValid && requestedOnValid && creatinineFieldsValid;
    };

    api.submit = function() {
        $.submit();
    }

    /* Function related to studies selection */

    api.selectStudy = function(study) {
        api.selectedStudies.push(study);
        api.studies.remove( function(item) { return item.id == study.id } );

    };

    api.unselectStudy = function(study) {
        api.studies.push(study);
        api.selectedStudies.remove( function(item) { return item.id == study.id } );
    };

    api.convertedStudies = function() {
        return $.map( api.studies(), function(element) {
            return { "label": element.name, "value": element.id };
        });
    };
    /* Functions related to portable */
    api.portable.subscribe(function(value) {
        if( !value ) {
            api.portableLocation(null);
        }
    });

    /* Functions related to contrast studies */
    api.selectedStudiesIncludeContrastStudy = function() {
//        for(var i = 0; i < api.selectedStudies().length; i++) {
//            if (contrastStudies.indexOf(api.selectedStudies()[i]["id"]) != -1) {
//                return true;
//            }
//        }
//        return false;
        return false;
    }

    for(var i=0; i<studies.length; i++) {
        api.studies.push( AutocompleteItem(studies[i]["value"],
            studies[i]["label"]) );
    }

    for(var i=0; i<locations.length; i++) {
        api.locations.push( AutocompleteItem(locations[i]["value"], locations[i]["label"]) );
    }

    return api;
}

ko.bindingHandlers.autocomplete = {
    init: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        $(element).autocomplete({
            source: function( request, response ) {
                var matcher = new RegExp( $.ui.autocomplete.escapeRegex( request.term ), "i" );
                response($.grep(allBindingsAccessor().search(), function (value) {
                    value = value.label || value.value || value;
                    return matcher.test(value) || matcher.test(emr.stripAccents(value));
                } ))
            },
            focus: function( event, ui ) {
                $(this).val(ui.item.label);
                return false;
            },
            select: function( event, ui ) {
                allBindingsAccessor().select(AutocompleteItem(ui.item.value, ui.item.label));
                if (allBindingsAccessor().clearValue()) {
                    $(this).val("");
                }
                return false;
            }
        });
    },
    update: function(element, valueAccessor, allBindingsAccessor, viewModel, bindingContext) {
        // It's invoked everytime we update the observable associated to the element
        // In this cases, we assume we'll always want to reset it
        $(element).val("");
    }
};
