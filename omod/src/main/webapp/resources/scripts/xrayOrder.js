function AutocompleteItem(id, name) {
    var api = {};
    api.id = id;
    api.name = name;

    return api;
}

function StudiesViewModel(studies, locations) {
    var api = {};
    api.searchTerm = ko.observable(true);
    api.selectedStudies = ko.observableArray([]);
    api.studies = ko.observableArray([]);

    api.portable = ko.observable(false);
    api.locations = ko.observableArray([])
    api.portableLocation = ko.observable();

    api.isValid = function() {
        var studiesAreValid = api.selectedStudies().length > 0;
        var portableIsValid = api.portable() ? api.portableLocation() != null : true;
        return studiesAreValid && portableIsValid;
    };

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
