define(["exports", "./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2"], function (t, m, i, _, g) {
    "use strict";

    function p(t, i, e) {
        if (0 === t) return i * e;
        var a = t * t, n = a * a, s = n * a, h = s * a, u = h * a, r = u * a, o = e;
        return i * ((1 - a / 4 - 3 * n / 64 - 5 * s / 256 - 175 * h / 16384 - 441 * u / 65536 - 4851 * r / 1048576) * o - (3 * a / 8 + 3 * n / 32 + 45 * s / 1024 + 105 * h / 4096 + 2205 * u / 131072 + 6237 * r / 524288) * Math.sin(2 * o) + (15 * n / 256 + 45 * s / 1024 + 525 * h / 16384 + 1575 * u / 65536 + 155925 * r / 8388608) * Math.sin(4 * o) - (35 * s / 3072 + 175 * h / 12288 + 3675 * u / 262144 + 13475 * r / 1048576) * Math.sin(6 * o) + (315 * h / 131072 + 2205 * u / 524288 + 43659 * r / 8388608) * Math.sin(8 * o) - (693 * u / 1310720 + 6237 * r / 5242880) * Math.sin(10 * o) + 1001 * r / 8388608 * Math.sin(12 * o))
    }

    function C(t, i) {
        if (0 === t) return Math.log(Math.tan(.5 * (_.CesiumMath.PI_OVER_TWO + i)));
        var e = t * Math.sin(i);
        return Math.log(Math.tan(.5 * (_.CesiumMath.PI_OVER_TWO + i))) - t / 2 * Math.log((1 + e) / (1 - e))
    }

    var f = new g.Cartesian3, P = new g.Cartesian3;

    function n(t, i, e, a) {
        g.Cartesian3.normalize(a.cartographicToCartesian(i, P), f), g.Cartesian3.normalize(a.cartographicToCartesian(e, P), P);
        var n, s, h, u, r, o, l, d = a.maximumRadius, M = a.minimumRadius, c = d * d, m = M * M;
        t._ellipticitySquared = (c - m) / c, t._ellipticity = Math.sqrt(t._ellipticitySquared), t._start = g.Cartographic.clone(i, t._start), t._start.height = 0, t._end = g.Cartographic.clone(e, t._end), t._end.height = 0, t._heading = (n = t, s = i.longitude, h = i.latitude, u = e.longitude, r = e.latitude, o = C(n._ellipticity, h), l = C(n._ellipticity, r), Math.atan2(_.CesiumMath.negativePiToPi(u - s), l - o)), t._distance = function (t, i, e, a, n, s, h) {
            var u = t._heading, r = s - a, o = 0;
            if (_.CesiumMath.equalsEpsilon(Math.abs(u), _.CesiumMath.PI_OVER_TWO, _.CesiumMath.EPSILON8)) if (i === e) o = i * Math.cos(n) * _.CesiumMath.negativePiToPi(r); else {
                var l = Math.sin(n);
                o = i * Math.cos(n) * _.CesiumMath.negativePiToPi(r) / Math.sqrt(1 - t._ellipticitySquared * l * l)
            } else {
                var d = p(t._ellipticity, i, n);
                o = (p(t._ellipticity, i, h) - d) / Math.cos(u)
            }
            return Math.abs(o)
        }(t, a.maximumRadius, a.minimumRadius, i.longitude, i.latitude, e.longitude, e.latitude)
    }

    function M(t, i, e, a, n, s) {
        var h, u, r, o = n * n;
        if (Math.abs(_.CesiumMath.PI_OVER_TWO - Math.abs(i)) > _.CesiumMath.EPSILON8) {
            u = function (t, i, e) {
                var a = t / e;
                if (0 === i) return a;
                var n = a * a, s = n * a, h = s * a, u = i * i, r = u * u, o = r * u, l = o * u, d = l * u, M = d * u,
                    c = Math.sin(2 * a), m = Math.cos(2 * a), _ = Math.sin(4 * a), g = Math.cos(4 * a),
                    p = Math.sin(6 * a), C = Math.cos(6 * a), f = Math.sin(8 * a), P = Math.cos(8 * a),
                    v = Math.sin(10 * a);
                return a + a * u / 4 + 7 * a * r / 64 + 15 * a * o / 256 + 579 * a * l / 16384 + 1515 * a * d / 65536 + 16837 * a * M / 1048576 + (3 * a * r / 16 + 45 * a * o / 256 - a * (32 * n - 561) * l / 4096 - a * (232 * n - 1677) * d / 16384 + a * (399985 - 90560 * n + 512 * h) * M / 5242880) * m + (21 * a * o / 256 + 483 * a * l / 4096 - a * (224 * n - 1969) * d / 16384 - a * (33152 * n - 112599) * M / 1048576) * g + (151 * a * l / 4096 + 4681 * a * d / 65536 + 1479 * a * M / 16384 - 453 * s * M / 32768) * C + (1097 * a * d / 65536 + 42783 * a * M / 1048576) * P + 8011 * a * M / 1048576 * Math.cos(10 * a) + (3 * u / 8 + 3 * r / 16 + 213 * o / 2048 - 3 * n * o / 64 + 255 * l / 4096 - 33 * n * l / 512 + 20861 * d / 524288 - 33 * n * d / 512 + h * d / 1024 + 28273 * M / 1048576 - 471 * n * M / 8192 + 9 * h * M / 4096) * c + (21 * r / 256 + 21 * o / 256 + 533 * l / 8192 - 21 * n * l / 512 + 197 * d / 4096 - 315 * n * d / 4096 + 584039 * M / 16777216 - 12517 * n * M / 131072 + 7 * h * M / 2048) * _ + (151 * o / 6144 + 151 * l / 4096 + 5019 * d / 131072 - 453 * n * d / 16384 + 26965 * M / 786432 - 8607 * n * M / 131072) * p + (1097 * l / 131072 + 1097 * d / 65536 + 225797 * M / 10485760 - 1097 * n * M / 65536) * f + (8011 * d / 2621440 + 8011 * M / 1048576) * v + 293393 * M / 251658240 * Math.sin(12 * a)
            }(p(n, a, t.latitude) + e * Math.cos(i), n, a);
            var l = C(n, t.latitude), d = C(n, u);
            r = Math.tan(i) * (d - l), h = _.CesiumMath.negativePiToPi(t.longitude + r)
        } else {
            var M;
            if (u = t.latitude, 0 === n) M = a * Math.cos(t.latitude); else {
                var c = Math.sin(t.latitude);
                M = a * Math.cos(t.latitude) / Math.sqrt(1 - o * c * c)
            }
            r = e / M, h = 0 < i ? _.CesiumMath.negativePiToPi(t.longitude + r) : _.CesiumMath.negativePiToPi(t.longitude - r)
        }
        return m.defined(s) ? (s.longitude = h, s.latitude = u, s.height = 0, s) : new g.Cartographic(h, u, 0)
    }

    function c(t, i, e) {
        var a = m.defaultValue(e, g.Ellipsoid.WGS84);
        this._ellipsoid = a, this._start = new g.Cartographic, this._end = new g.Cartographic, this._heading = void 0, this._distance = void 0, this._ellipticity = void 0, this._ellipticitySquared = void 0, m.defined(t) && m.defined(i) && n(this, t, i, a)
    }

    Object.defineProperties(c.prototype, {
        ellipsoid: {
            get: function () {
                return this._ellipsoid
            }
        }, surfaceDistance: {
            get: function () {
                return this._distance
            }
        }, start: {
            get: function () {
                return this._start
            }
        }, end: {
            get: function () {
                return this._end
            }
        }, heading: {
            get: function () {
                return this._heading
            }
        }
    }), c.fromStartHeadingDistance = function (t, i, e, a, n) {
        var s = m.defaultValue(a, g.Ellipsoid.WGS84), h = s.maximumRadius, u = s.minimumRadius, r = h * h, o = u * u,
            l = Math.sqrt((r - o) / r), d = M(t, i = _.CesiumMath.negativePiToPi(i), e, s.maximumRadius, l);
        return !m.defined(n) || m.defined(a) && !a.equals(n.ellipsoid) ? new c(t, d, s) : (n.setEndPoints(t, d), n)
    }, c.prototype.setEndPoints = function (t, i) {
        n(this, t, i, this._ellipsoid)
    }, c.prototype.interpolateUsingFraction = function (t, i) {
        return this.interpolateUsingSurfaceDistance(t * this._distance, i)
    }, c.prototype.interpolateUsingSurfaceDistance = function (t, i) {
        return M(this._start, this._heading, t, this._ellipsoid.maximumRadius, this._ellipticity, i)
    }, c.prototype.findIntersectionWithLongitude = function (t, i) {
        var e = this._ellipticity, a = this._heading, n = Math.abs(a), s = this._start;
        if (t = _.CesiumMath.negativePiToPi(t), _.CesiumMath.equalsEpsilon(Math.abs(t), Math.PI, _.CesiumMath.EPSILON14) && (t = _.CesiumMath.sign(s.longitude) * Math.PI), m.defined(i) || (i = new g.Cartographic), Math.abs(_.CesiumMath.PI_OVER_TWO - n) <= _.CesiumMath.EPSILON8) return i.longitude = t, i.latitude = s.latitude, i.height = 0, i;
        if (_.CesiumMath.equalsEpsilon(Math.abs(_.CesiumMath.PI_OVER_TWO - n), _.CesiumMath.PI_OVER_TWO, _.CesiumMath.EPSILON8)) {
            if (_.CesiumMath.equalsEpsilon(t, s.longitude, _.CesiumMath.EPSILON12)) return;
            return i.longitude = t, i.latitude = _.CesiumMath.PI_OVER_TWO * _.CesiumMath.sign(_.CesiumMath.PI_OVER_TWO - a), i.height = 0, i
        }
        var h, u = s.latitude, r = e * Math.sin(u),
            o = Math.tan(.5 * (_.CesiumMath.PI_OVER_TWO + u)) * Math.exp((t - s.longitude) / Math.tan(a)),
            l = (1 + r) / (1 - r), d = s.latitude;
        do {
            h = d;
            var M = e * Math.sin(h), c = (1 + M) / (1 - M);
            d = 2 * Math.atan(o * Math.pow(c / l, e / 2)) - _.CesiumMath.PI_OVER_TWO
        } while (!_.CesiumMath.equalsEpsilon(d, h, _.CesiumMath.EPSILON12));
        return i.longitude = t, i.latitude = d, i.height = 0, i
    }, c.prototype.findIntersectionWithLatitude = function (t, i) {
        var e = this._ellipticity, a = this._heading, n = this._start;
        if (!_.CesiumMath.equalsEpsilon(Math.abs(a), _.CesiumMath.PI_OVER_TWO, _.CesiumMath.EPSILON8)) {
            var s = C(e, n.latitude), h = C(e, t), u = Math.tan(a) * (h - s),
                r = _.CesiumMath.negativePiToPi(n.longitude + u);
            return m.defined(i) ? (i.longitude = r, i.latitude = t, i.height = 0, i) : new g.Cartographic(r, t, 0)
        }
    }, t.EllipsoidRhumbLine = c
});
