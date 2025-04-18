define(["exports", "./when-c2e8ef35", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./ComponentDatatype-5d3f6452", "./GeometryAttribute-4098b8de", "./GeometryAttributes-57608efc", "./GeometryPipeline-99c06fbd", "./IndexDatatype-e3260434", "./arrayRemoveDuplicates-a580a060", "./ArcType-29cf2197", "./EllipsoidRhumbLine-5134246a", "./PolygonPipeline-84f0d07f"], function (e, I, x, E, y, A, P, _, d, G, L, M, v, D) {
    "use strict";

    function S() {
        this._array = [], this._offset = 0, this._length = 0
    }

    Object.defineProperties(S.prototype, {
        length: {
            get: function () {
                return this._length
            }
        }
    }), S.prototype.enqueue = function (e) {
        this._array.push(e), this._length++
    }, S.prototype.dequeue = function () {
        if (0 !== this._length) {
            var e = this._array, t = this._offset, r = e[t];
            return e[t] = void 0, 10 < ++t && 2 * t > e.length && (this._array = e.slice(t), t = 0), this._offset = t, this._length--, r
        }
    }, S.prototype.peek = function () {
        if (0 !== this._length) return this._array[this._offset]
    }, S.prototype.contains = function (e) {
        return -1 !== this._array.indexOf(e)
    }, S.prototype.clear = function () {
        this._array.length = this._offset = this._length = 0
    }, S.prototype.sort = function (e) {
        0 < this._offset && (this._array = this._array.slice(this._offset), this._offset = 0), this._array.sort(e)
    };
    var R = {
        computeHierarchyPackedLength: function (e) {
            for (var t = 0, r = [e]; 0 < r.length;) {
                var i = r.pop();
                if (I.defined(i)) {
                    t += 2;
                    var n = i.positions, a = i.holes;
                    if (I.defined(n) && (t += n.length * E.Cartesian3.packedLength), I.defined(a)) for (var o = a.length, s = 0; s < o; ++s) r.push(a[s])
                }
            }
            return t
        }, packPolygonHierarchy: function (e, t, r) {
            for (var i = [e]; 0 < i.length;) {
                var n = i.pop();
                if (I.defined(n)) {
                    var a = n.positions, o = n.holes;
                    if (t[r++] = I.defined(a) ? a.length : 0, t[r++] = I.defined(o) ? o.length : 0, I.defined(a)) for (var s = a.length, u = 0; u < s; ++u, r += 3) E.Cartesian3.pack(a[u], t, r);
                    if (I.defined(o)) for (var l = o.length, h = 0; h < l; ++h) i.push(o[h])
                }
            }
            return r
        }, unpackPolygonHierarchy: function (e, t) {
            for (var r = e[t++], i = e[t++], n = new Array(r), a = 0 < i ? new Array(i) : void 0, o = 0; o < r; ++o, t += E.Cartesian3.packedLength) n[o] = E.Cartesian3.unpack(e, t);
            for (var s = 0; s < i; ++s) a[s] = R.unpackPolygonHierarchy(e, t), t = a[s].startingIndex, delete a[s].startingIndex;
            return {positions: n, holes: a, startingIndex: t}
        }
    }, g = new E.Cartesian3;
    R.subdivideLineCount = function (e, t, r) {
        var i = E.Cartesian3.distance(e, t) / r, n = Math.max(0, Math.ceil(x.CesiumMath.log2(i)));
        return Math.pow(2, n)
    };
    var m = new E.Cartographic, C = new E.Cartographic, b = new E.Cartographic, w = new E.Cartesian3;
    R.subdivideRhumbLineCount = function (e, t, r, i) {
        var n = e.cartesianToCartographic(t, m), a = e.cartesianToCartographic(r, C),
            o = new v.EllipsoidRhumbLine(n, a, e).surfaceDistance / i, s = Math.max(0, Math.ceil(x.CesiumMath.log2(o)));
        return Math.pow(2, s)
    }, R.subdivideLine = function (e, t, r, i) {
        var n = R.subdivideLineCount(e, t, r), a = E.Cartesian3.distance(e, t), o = a / n;
        I.defined(i) || (i = []);
        var s = i;
        s.length = 3 * n;
        for (var u, l, h, c, f = 0, p = 0; p < n; p++) {
            var d = (u = e, l = t, h = p * o, c = a, E.Cartesian3.subtract(l, u, g), E.Cartesian3.multiplyByScalar(g, h / c, g), E.Cartesian3.add(u, g, g), [g.x, g.y, g.z]);
            s[f++] = d[0], s[f++] = d[1], s[f++] = d[2]
        }
        return s
    }, R.subdivideRhumbLine = function (e, t, r, i, n) {
        var a = e.cartesianToCartographic(t, m), o = e.cartesianToCartographic(r, C),
            s = new v.EllipsoidRhumbLine(a, o, e), u = s.surfaceDistance / i,
            l = Math.max(0, Math.ceil(x.CesiumMath.log2(u))), h = Math.pow(2, l), c = s.surfaceDistance / h;
        I.defined(n) || (n = []);
        var f = n;
        f.length = 3 * h;
        for (var p = 0, d = 0; d < h; d++) {
            var y = s.interpolateUsingSurfaceDistance(d * c, b), g = e.cartographicToCartesian(y, w);
            f[p++] = g.x, f[p++] = g.y, f[p++] = g.z
        }
        return f
    };
    var f = new E.Cartesian3, p = new E.Cartesian3, T = new E.Cartesian3, N = new E.Cartesian3;
    R.scaleToGeodeticHeightExtruded = function (e, t, r, i, n) {
        i = I.defaultValue(i, E.Ellipsoid.WGS84);
        var a = f, o = p, s = T, u = N;
        if (I.defined(e) && I.defined(e.attributes) && I.defined(e.attributes.position)) for (var l = e.attributes.position.values, h = l.length / 2, c = 0; c < h; c += 3) E.Cartesian3.fromArray(l, c, s), i.geodeticSurfaceNormal(s, a), u = i.scaleToGeodeticSurface(s, u), o = E.Cartesian3.multiplyByScalar(a, r, o), o = E.Cartesian3.add(u, o, o), l[c + h] = o.x, l[c + 1 + h] = o.y, l[c + 2 + h] = o.z, n && (u = E.Cartesian3.clone(s, u)), o = E.Cartesian3.multiplyByScalar(a, t, o), o = E.Cartesian3.add(u, o, o), l[c] = o.x, l[c + 1] = o.y, l[c + 2] = o.z;
        return e
    }, R.polygonOutlinesFromHierarchy = function (e, t, r) {
        var i, n, a, o = [], s = new S;
        for (s.enqueue(e); 0 !== s.length;) {
            var u = s.dequeue(), l = u.positions;
            if (t) for (a = l.length, i = 0; i < a; i++) r.scaleToGeodeticSurface(l[i], l[i]);
            if (!((l = L.arrayRemoveDuplicates(l, E.Cartesian3.equalsEpsilon, !0)).length < 3)) {
                var h = u.holes ? u.holes.length : 0;
                for (i = 0; i < h; i++) {
                    var c = u.holes[i], f = c.positions;
                    if (t) for (a = f.length, n = 0; n < a; ++n) r.scaleToGeodeticSurface(f[n], f[n]);
                    if (!((f = L.arrayRemoveDuplicates(f, E.Cartesian3.equalsEpsilon, !0)).length < 3)) {
                        o.push(f);
                        var p = 0;
                        for (I.defined(c.holes) && (p = c.holes.length), n = 0; n < p; n++) s.enqueue(c.holes[n])
                    }
                }
                o.push(l)
            }
        }
        return o
    }, R.polygonsFromHierarchy = function (e, t, r, i) {
        var n = [], a = [], o = new S;
        for (o.enqueue(e); 0 !== o.length;) {
            var s, u, l = o.dequeue(), h = l.positions, c = l.holes;
            if (r) for (u = h.length, s = 0; s < u; s++) i.scaleToGeodeticSurface(h[s], h[s]);
            if (!((h = L.arrayRemoveDuplicates(h, E.Cartesian3.equalsEpsilon, !0)).length < 3)) {
                var f = t(h);
                if (I.defined(f)) {
                    var p = [], d = D.PolygonPipeline.computeWindingOrder2D(f);
                    d === D.WindingOrder.CLOCKWISE && (f.reverse(), h = h.slice().reverse());
                    var y, g = h.slice(), v = I.defined(c) ? c.length : 0, m = [];
                    for (s = 0; s < v; s++) {
                        var C = c[s], b = C.positions;
                        if (r) for (u = b.length, y = 0; y < u; ++y) i.scaleToGeodeticSurface(b[y], b[y]);
                        if (!((b = L.arrayRemoveDuplicates(b, E.Cartesian3.equalsEpsilon, !0)).length < 3)) {
                            var w = t(b);
                            if (I.defined(w)) {
                                (d = D.PolygonPipeline.computeWindingOrder2D(w)) === D.WindingOrder.CLOCKWISE && (w.reverse(), b = b.slice().reverse()), m.push(b), p.push(g.length), g = g.concat(b), f = f.concat(w);
                                var T = 0;
                                for (I.defined(C.holes) && (T = C.holes.length), y = 0; y < T; y++) o.enqueue(C.holes[y])
                            }
                        }
                    }
                    n.push({outerRing: h, holes: m}), a.push({positions: g, positions2D: f, holes: p})
                }
            }
        }
        return {hierarchy: n, polygons: a}
    };
    var O = new E.Cartesian2, q = new E.Cartesian3, B = new y.Quaternion, H = new y.Matrix3;
    R.computeBoundingRectangle = function (e, t, r, i, n) {
        for (var a = y.Quaternion.fromAxisAngle(e, i, B), o = y.Matrix3.fromQuaternion(a, H), s = Number.POSITIVE_INFINITY, u = Number.NEGATIVE_INFINITY, l = Number.POSITIVE_INFINITY, h = Number.NEGATIVE_INFINITY, c = r.length, f = 0; f < c; ++f) {
            var p = E.Cartesian3.clone(r[f], q);
            y.Matrix3.multiplyByVector(o, p, p);
            var d = t(p, O);
            I.defined(d) && (s = Math.min(s, d.x), u = Math.max(u, d.x), l = Math.min(l, d.y), h = Math.max(h, d.y))
        }
        return n.x = s, n.y = l, n.width = u - s, n.height = h - l, n
    }, R.createGeometryFromPositions = function (e, t, r, i, n, a) {
        var o = D.PolygonPipeline.triangulate(t.positions2D, t.holes);
        o.length < 3 && (o = [0, 1, 2]);
        var s = t.positions;
        if (i) {
            for (var u = s.length, l = new Array(3 * u), h = 0, c = 0; c < u; c++) {
                var f = s[c];
                l[h++] = f.x, l[h++] = f.y, l[h++] = f.z
            }
            var p = new P.Geometry({
                attributes: {
                    position: new P.GeometryAttribute({
                        componentDatatype: A.ComponentDatatype.DOUBLE,
                        componentsPerAttribute: 3,
                        values: l
                    })
                }, indices: o, primitiveType: P.PrimitiveType.TRIANGLES
            });
            return n.normal ? d.GeometryPipeline.computeNormal(p) : p
        }
        return a === M.ArcType.GEODESIC ? D.PolygonPipeline.computeSubdivision(e, s, o, r) : a === M.ArcType.RHUMB ? D.PolygonPipeline.computeRhumbLineSubdivision(e, s, o, r) : void 0
    };
    var k = [], z = new E.Cartesian3, W = new E.Cartesian3;
    R.computeWallGeometry = function (e, t, r, i, n) {
        var a, o, s, u, l, h = e.length, c = 0;
        if (i) for (o = 3 * h * 2, a = new Array(2 * o), s = 0; s < h; s++) u = e[s], l = e[(s + 1) % h], a[c] = a[c + o] = u.x, a[++c] = a[c + o] = u.y, a[++c] = a[c + o] = u.z, a[++c] = a[c + o] = l.x, a[++c] = a[c + o] = l.y, a[++c] = a[c + o] = l.z, ++c; else {
            var f = x.CesiumMath.chordLength(r, t.maximumRadius), p = 0;
            if (n === M.ArcType.GEODESIC) for (s = 0; s < h; s++) p += R.subdivideLineCount(e[s], e[(s + 1) % h], f); else if (n === M.ArcType.RHUMB) for (s = 0; s < h; s++) p += R.subdivideRhumbLineCount(t, e[s], e[(s + 1) % h], f);
            for (o = 3 * (p + h), a = new Array(2 * o), s = 0; s < h; s++) {
                var d;
                u = e[s], l = e[(s + 1) % h], n === M.ArcType.GEODESIC ? d = R.subdivideLine(u, l, f, k) : n === M.ArcType.RHUMB && (d = R.subdivideRhumbLine(t, u, l, f, k));
                for (var y = d.length, g = 0; g < y; ++g, ++c) a[c] = d[g], a[c + o] = d[g];
                a[c] = l.x, a[c + o] = l.x, a[++c] = l.y, a[c + o] = l.y, a[++c] = l.z, a[c + o] = l.z, ++c
            }
        }
        h = a.length;
        var v = G.IndexDatatype.createTypedArray(h / 3, h - 6 * e.length), m = 0;
        for (h /= 6, s = 0; s < h; s++) {
            var C = s, b = C + 1, w = C + h, T = w + 1;
            u = E.Cartesian3.fromArray(a, 3 * C, z), l = E.Cartesian3.fromArray(a, 3 * b, W), E.Cartesian3.equalsEpsilon(u, l, x.CesiumMath.EPSILON10, x.CesiumMath.EPSILON10) || (v[m++] = C, v[m++] = w, v[m++] = b, v[m++] = b, v[m++] = w, v[m++] = T)
        }
        return new P.Geometry({
            attributes: new _.GeometryAttributes({
                position: new P.GeometryAttribute({
                    componentDatatype: A.ComponentDatatype.DOUBLE,
                    componentsPerAttribute: 3,
                    values: a
                })
            }), indices: v, primitiveType: P.PrimitiveType.TRIANGLES
        })
    }, e.PolygonGeometryLibrary = R
});
