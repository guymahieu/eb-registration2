const timeBlockOk = {
    messages: {
        en: function (field, args) {
            return 'het gekozen tijdstip zit al te vol voor uw reservatie, selecteer een ander tijdstip';
        },
        nl: function (field, args) {
            return 'het gekozen tijdstip zit al te vol voor uw reservatie, selecteer een ander tijdstip';
        }
    },
    validate: function (value, args) {
        var timeBlockKey = value;
        var reservation = args[0];
        var timeBlocksByKey = args[1];

        var nrOfPeopleInReservation = reservation.nrOfAdults + reservation.nrOfChildren;
        var selectedTimeblock = timeBlocksByKey[timeBlockKey];
        if (!selectedTimeblock) {
            return false;
        }
        return nrOfPeopleInReservation <= selectedTimeblock.freePlaces;
    }
};

const specialWishesCount = {
    messages: {
        en: function (field, args) {
            return 'het aantal ' + field + ' voor vegetarisch en/of halal is hoger dan het totaal aantal dat u aangaf';
        },
        nl: function (field, args) {
            return 'het aantal ' + field + ' voor vegetarisch en/of halal is hoger dan het totaal aantal dat u aangaf';
        }
    },
    validate: function (value, args) {
        var reservation = args[0];
        if (args[1] === 'kinderen') {
            return ((reservation.nrOfChildren || 0) >= ((reservation.nrOfChildrenVeggie || 0) + (reservation.nrOfChildrenHalal || 0)));
        }
        if (args[1] === 'volwassenen') {
            return ((reservation.nrOfAdults || 0) >= ((reservation.nrOfAdultsVeggie || 0) + (reservation.nrOfAdultsHalal || 0)));
        }
        return true;
    }
};

VeeValidate.Validator.extend('timeBlock', timeBlockOk);
VeeValidate.Validator.extend('specialWishesCount', specialWishesCount);

Vue.use(VeeValidate, {locale: 'nl'});


var locale = {
    name: 'nl',
    messages: {
        _default: function (field) {
            return field + ' waarde is ongeldig.'
        },
        email: function (field) {
            return field + ' moet een geldig emailadres zijn'
        },
        max: function (field, length) {
            return field + ' mag niet groter zijn dan ' + length + ' karakters.'
        },
        max_value: function (field, max) {
            return field + ' moet maximaal ' + max + ' zijn.'
        },
        min: function (field, length) {
            return field + ' moet minimaal ' + length + ' karakters lang zijn.'
        },
        min_value: function (field, min) {
            return field + ' moet minimaal ' + min + ' zijn.'
        },
        numeric: function (field) {
            return field + ' mag alleen nummers bevatten'
        },
        regex: function (field) {
            return field + ' formaat is ongeldig.'
        },
        required: function (field) {
            return field + ' is verplicht.'
        },
    },
    attributes: {}
};

VeeValidate.Validator.addLocale(locale);

var app = new Vue({
    el: '#app',
    data: {
        timeblocksByKey: {},
        timeblocks: undefined,
        price: {
            child: 5,
            adult: 7
        },
        reservation: {
            firstname: undefined,
            lastname: undefined,
            emailAddress: undefined,
            phoneNumber: undefined,
            timeBlock: undefined,
            nrOfChildren: 0,
            nrOfChildrenHalal: undefined,
            nrOfChildrenVeggie: undefined,
            nrOfAdults: 0,
            nrOfAdultsHalal: undefined,
            nrOfAdultsVeggie: undefined
        },
        nobodyComing: undefined,
        specialWishes: {
            halal: false,
            veggie: false,
            childCountError: undefined,
            adultCountError: undefined,
        },
        webflow: {
            stage: 'initial'
        },
        bill: undefined,
        apiUrlPrefix: '',
        submitting: false,
        loadTimeblocksFailed: false,
        submitFailed: false
    },
    mounted: function () {
        if (window.location.hostname === 'localhost') {
            this.apiUrlPrefix = 'http://localhost:1234'
        }
        this.refreshTimeBlocks();
    },
    methods: {
        // flow: initial -> form -> confirm --(save)--> done
        refreshTimeBlocks: function (e) {
            var self = this;
            self.loadTimeblocksFailed = false;
            $.get(self.apiUrlPrefix + '/api/timeblocks?_=' + new Date().getTime(), function (data) {
                self.populateTimeBlocks(data);
                self.loadTimeblocksFailed = false;
            }).fail(function () {
                self.loadTimeblocksFailed = true;
            });
        },
        populateTimeBlocks: function (timeblocksFromServer) {
            var self = this;
            self.timeblocks = timeblocksFromServer;
            self.timeblocksByKey = timeblocksFromServer.reduce(function (map, obj) {
                map[obj.key] = obj;
                return map;
            }, {});
            self.loadTimeblocksFailed = false;
        },
        correctCounts: function () {
            var self = this;
            self.reservation.nrOfAdults = self.reservation.nrOfAdults || 0;
            self.reservation.nrOfChildren = self.reservation.nrOfChildren || 0;
            if (!self.specialWishes.halal) {
                self.reservation.nrOfAdultsHalal = 0;
                self.reservation.nrOfChildrenHalal = 0;
            }
            if (!self.specialWishes.veggie) {
                self.reservation.nrOfAdultsVeggie = 0;
                self.reservation.nrOfChildrenVeggie = 0;
            }
        },
        validateSpecialWishesCounts: function () {
            var self = this;
            self.correctCounts();
            var isValid = true;
            if ((self.reservation.nrOfChildren + self.reservation.nrOfAdults) === 0) {
                self.nobodyComing = 'U heeft geen deelnemersaantallen opgegeven.';
                isValid = false;
            } else {
                self.nobodyComing = undefined;
            }
            if (self.reservation.nrOfAdults < self.reservation.nrOfAdultsVeggie + self.reservation.nrOfAdultsHalal) {
                self.specialWishes.adultCountError = 'het aantal volwassenen voor vegetarisch en/of halal is hoger dan het totaal aantal dat u aangaf';
                isValid = false;
            } else {
                self.specialWishes.adultCountError = undefined;
            }
            if (self.reservation.nrOfChildren < self.reservation.nrOfChildrenVeggie + self.reservation.nrOfChildrenHalal) {
                self.specialWishes.childCountError = 'het aantal kinderen voor vegetarisch en/of halal is hoger dan het totaal aantal dat u aangaf';
                isValid = false;
            } else {
                self.specialWishes.childCountError = undefined;
            }
            return isValid;
        },
        validateFormAndGoToConfirmation: function (e) {
            var self = this;
            self.submitFailed = false;
            var countsOk = self.validateSpecialWishesCounts();
            self.timeBlockFullError = false;
            this.$validator.validateAll().then(function (result) {
                if (countsOk && result) {
                    self.webflow.stage = 'confirm';
                } else {
                    self.submitFailed = true;
                }
            });
        },
        submitReservation: function (e) {
            var self = this;
            self.submitting = true;
            self.submitFailed = false;
            self.timeBlockFullError = false;
            var countsOk = self.validateSpecialWishesCounts();
            this.$validator.validateAll().then(function (result) {
                if (countsOk && result) {
                    $.post(self.apiUrlPrefix + '/api/reservation?_=' + new Date().getTime(), JSON.stringify(self.reservation))
                        .done(function (data, status, xhr) {
                            self.bill = data;
                            self.submitting = false;
                            self.webflow.stage = 'done';
                        })
                        .fail(function (xhr, status, error) {
                            if (xhr.status === 400) {
                                var errorData = JSON.parse(xhr.responseText);
                                self.populateTimeBlocks(errorData.refreshedTimeBlocks);
                                self.timeBlockFullError = errorData.errorMessage;
                                self.webflow.stage = 'confirm';
                                self.submitting = false;
                            } else {
                                self.webflow.stage = 'form';
                                self.submitFailed = true;
                                self.submitting = false;
                            }
                        });
                } else {
                    self.webflow.stage = 'form';
                    self.submitFailed = true;
                    self.submitting = false;
                }
            });
        },
        generateStartTimeOption: function (reservation, timeblock) {
            if (timeblock.freePlaces === 0) {
                return timeblock.displayValue + ' (volzet)';
            } else if (timeblock.freePlaces > 0 && (reservation.nrOfAdults + reservation.nrOfChildren) > timeblock.freePlaces) {
                var plaatsenText = (timeblock.freePlaces === 1) ? 'plaats' : 'plaatsen';
                return timeblock.displayValue + ' (slechts ' + timeblock.freePlaces + ' ' + plaatsenText + ' vrij)';
            } else {
                var plaatsenText = (timeblock.freePlaces === 1) ? 'plaats' : 'plaatsen';
                return timeblock.displayValue + ' (' + timeblock.freePlaces + ' ' + plaatsenText + ' vrij)';
            }
        }
    }

});
