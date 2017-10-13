new Vue({
    el: '#app',
    data: {
        apiUrlPrefix: '',
        loading: false,
        loadingFailed: false,
        paymentFailed: false,
        reservations: [],
        login: {
            ok: false,
            error: false,
            user: undefined,
            password: undefined
        }
    },
    mounted: function () {
        if (window.location.hostname === 'localhost') {
            this.apiUrlPrefix = 'http://localhost:1234'
        }
    },
    methods: {
        refreshReservations: function (e) {
            var self = this;
            self.loading = true;
            self.loadingFailed = false;
            $.ajax({
                url: self.apiUrlPrefix + '/api/backoffice?_=' + new Date().getTime(),
                type: 'GET',
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Basic ' + btoa(self.login.user + ':' + self.login.password));
                }
            })
                .done(function (data, status, xhr) {
                    self.login.ok = true;
                    self.login.error = false;
                    self.reservations = data;
                    self.reservations.forEach(function (reservation) {
                        reservation.createdAt = new Date(reservation.createdAt);
                        reservation.ageInDays = self.daysSince(reservation.createdAt);
                    });
                    self.loadingFailed = false;
                })
                .fail(function (xhr, status, error) {
                    if (xhr.status === 401 || xhr.status === 403) {
                        self.login.ok = false;
                        self.login.error = true;
                    }
                });

        },
        registerPayment: function (reservation, paid) {
            var self = this;
            self.paymentFailed = false;
            $.ajax({
                url: self.apiUrlPrefix + '/api/backoffice',
                type: 'POST',
                // contentType: 'application/json',
                data: {"externalId": reservation.externalId, "paid": paid},
                beforeSend: function (xhr) {
                    xhr.setRequestHeader('Authorization', 'Basic ' + btoa(self.login.user + ':' + self.login.password));
                }
            }).done(function (data, status, xhr) {
                self.reservations.forEach(function (r) {
                    if (r.externalId === reservation.externalId) {
                        r.paid = paid;
                    }
                });
                self.paymentFailed = false;
            })
                .fail(function (xhr, status, error) {
                    if (xhr.status === 401 || xhr.status === 403) {
                        self.login.ok = false;
                        self.login.error = true;
                    } else {
                        self.paymentFailed = true;
                    }
                });

        },
        daysSince: function (date) {
            var oneDay = 24 * 60 * 60 * 1000; // hours*minutes*seconds*milliseconds
            return Math.round(Math.abs((new Date().getTime() - date.getTime()) / (oneDay)));
        }
    }

})
;
