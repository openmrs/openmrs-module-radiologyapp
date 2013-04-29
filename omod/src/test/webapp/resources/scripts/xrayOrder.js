describe("X-ray studies selection", function() {
    var firstStudy = AutocompleteItem(1, "First Study");
    var studies = [
        {"value": 1, "label": "First Study"},
        {"value": 2, "label": "Second Study"},
        {"value": 3, "label": "Third Study"}];

    var emergencyLocation = AutocompleteItem(1, "Emergency");
    var locations = [
        {"value": 1, "label": "Emergency"},
        {"value": 2, "label": "Clinics"},
        {"value": 3, "label": "Sant Femme"}];

    var viewModel = StudiesViewModel(studies, locations);


    it("should initialize correctly", function() {
        expect(viewModel.studies().length).toBe(3);
        expect(viewModel.selectedStudies().length).toBe(0);

        expect(viewModel.portable(), false);
        expect(viewModel.portableLocation()).toBe(undefined);
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

    it("should asses that the viewModel without selected studies is not valid", function() {
        expect(viewModel.selectedStudies().length).toBe(0);
        expect(viewModel.isValid()).toBe(false);
    });

    it("should asses that viewModel with selected study, marked as portable without a location is not valid", function() {
        viewModel.selectStudy(firstStudy);
        viewModel.portable(true);
        expect(viewModel.isValid()).toBe(false);
    });

    it("should asses that viewModel with selected study, marked as portable with a location is valid", function() {
        viewModel.selectStudy(firstStudy);
        viewModel.portable(true);
        viewModel.portableLocation(emergencyLocation);
        expect(viewModel.isValid()).toBe(true);
    });

    it("should asses that viewModel not portable is valid", function() {
        viewModel.selectStudy(firstStudy);
        viewModel.portable(false);
        expect(viewModel.isValid()).toBe(true);
    });

})