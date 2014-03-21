describe("X-ray studies selection", function() {
    var firstStudy = AutocompleteItem(1, "First Study");
    var secondStudy = AutocompleteItem(2, "First Study");
    var studies = [
        {"value": 1, "label": "First Study"},
        {"value": 2, "label": "Second Study"},
        {"value": 3, "label": "Third Study"}];

    var emergencyLocation = AutocompleteItem(1, "Emergency");
    var locations = [
        {"value": 1, "label": "Emergency"},
        {"value": 2, "label": "Clinics"},
        {"value": 3, "label": "Sant Femme"}];

    var contrastStudies = [ 2 ];
    var viewModel;


    beforeEach(function() {
        viewModel = StudiesViewModel(studies, locations, [], contrastStudies);
    })

    it("should initialize correctly", function() {
        expect(viewModel.studies().length).toBe(3);
        expect(viewModel.selectedStudies().length).toBe(0);

        expect(viewModel.portable(), false);
        expect(viewModel.portableLocation()).toBe(undefined);
        expect(viewModel.selectedStudiesIncludeContrastStudy()).toBe(false);
    });

    it("should select and deselect a study", function() {
        viewModel.selectStudy(firstStudy);

        expect(viewModel.studies().length).toBe(2);
        expect(viewModel.selectedStudies().length).toBe(1);
        expect(viewModel.selectedStudies()[0].name).toBe("First Study");

        viewModel.unselectStudy(firstStudy);
        expect(viewModel.studies().length).toBe(3);
        expect(viewModel.selectedStudies().length).toBe(0);
    });

    it("should clear portable location after setting not portable", function() {
        viewModel.portable(true);
        viewModel.portableLocation(emergencyLocation);
        viewModel.portable(false);
        expect(viewModel.portableLocation()).toBe(null);
    });

    it("should assess that the viewModel without selected studies is not valid", function() {
        expect(viewModel.selectedStudies().length).toBe(0);
        expect(viewModel.isValid()).toBe(false);
    });

    it("should assess that viewModel with selected study, marked as portable without a location is not valid", function() {
        viewModel.selectStudy(firstStudy);
        viewModel.portable(true);
        expect(viewModel.isValid()).toBe(false);
    });

    it("should assess that viewModel with selected study, marked as portable with a location is valid", function() {
        viewModel.selectStudy(firstStudy);
        viewModel.portable(true);
        viewModel.portableLocation(emergencyLocation);
        expect(viewModel.isValid()).toBe(true);
    });

    it("should assess that viewModel not portable is valid", function() {
        viewModel.selectStudy(firstStudy);
        viewModel.portable(false);
        expect(viewModel.isValid()).toBe(true);
    });

    it("should assess that viewModel without clinical history is not valid if clinical history marked required", function() {
        viewModel.requiredFields = ['clinicalHistory'];
        viewModel.selectStudy(firstStudy);
        viewModel.portable(false);
        expect(viewModel.isValid()).toBe(false);
    });

    it("should assess that viewModel clinical history is valid", function() {
        viewModel.requiredFields = ['clinicalHistory'];
        viewModel.clinicalHistory("some history");
        viewModel.selectStudy(firstStudy);
        viewModel.portable(false);
        expect(viewModel.isValid()).toBe(true);
    });

    it("should select set selected studies include contrast study if contrast study selected", function() {
        viewModel.selectStudy(secondStudy);
        expect(viewModel.selectedStudiesIncludeContrastStudy()).toBe(true);

        viewModel.unselectStudy(secondStudy);
        expect(viewModel.selectedStudiesIncludeContrastStudy()).toBe(false);
    });

    it("should assess that viewModel is not valid if study includes contrast but creatinine fields not entered", function() {
        viewModel.selectStudy(secondStudy);
        expect(viewModel.isValid()).toBe(false);
    })

    it("should assess that viewModel is valid if study includes contrast an creatinine fields not entered", function() {
        viewModel.selectStudy(secondStudy);
        viewModel.creatinineLevel("1")
        viewModel.creatinineTestDate("some date");
        expect(viewModel.isValid()).toBe(true);
    })

})