
function loadRadiologyTemplates(displayStudyOrderNumber) {

    var radiologyStudyDetailsTemplate= _.template(jq('#radiologyStudyDetailsTemplate').html());

    var radiologyStudyDetailsSection = $('#study-details');

    function loadRadiologyStudy(studyElement) {
        var studyOrderNumber = studyElement.attr('studyOrderNumber');

        if (studyOrderNumber != undefined) {
            radiologyStudyDetailsSection.html("<i class=\"icon-spinner icon-spin icon-2x pull-left\"></i>");

            $.getJSON(
                emr.fragmentActionLink("radiologyapp", "radiologyTab", "getRadiologyStudyByOrderNumber", {
                    studyOrderNumber: studyOrderNumber
                })
            ).success(function(data) {

                $('.viewRadiologyStudyDetails').removeClass('selected');
                studyElement.addClass('selected');

                radiologyStudyDetailsSection.html(radiologyStudyDetailsTemplate(data));
                radiologyStudyDetailsSection.show();

            }).error(function(err) {
                emr.errorMessage(err)
            });
        }
    }

    // load the first study
    studyOrderElement = null;
    if (displayStudyOrderNumber != null && displayStudyOrderNumber != 'null') {
        studyOrderElement = $('.viewRadiologyStudyDetails[studyordernumber=' + displayStudyOrderNumber + ']');
    }
    if ( typeof studyOrderElement !== undefined && studyOrderElement !== null) {
        loadRadiologyStudy(studyOrderElement);
    } else {
        loadRadiologyStudy($('.viewRadiologyStudyDetails').first());
    }

    // register click handlers for loading other studies
    $('.viewRadiologyStudyDetails').click(function() {
        loadRadiologyStudy($(this));
        return false;
    });
}