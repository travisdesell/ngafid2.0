define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./Cartesian2-e875d9d2", "./Transforms-44592b02", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./AttributeCompression-6cb5b251", "./IndexDatatype-e3260434", "./IntersectionTests-ef7d18d8", "./Plane-2d882f9f", "./createTaskProcessorWorker", "./EllipsoidTangentPlane-30395e74", "./OrientedBoundingBox-e6450288", "./TerrainEncoding-32e7a288"], function (w, e, ge, ce, me, t, i, n, s, xe, r, h, u, o, ve, we) {
    "use strict";
    var Ce = {
            clipTriangleAtAxisAlignedThreshold: function (e, t, i, n, s, r) {
                var h, u, o;
                w.defined(r) ? r.length = 0 : r = [], o = t ? (h = i < e, u = n < e, s < e) : (h = e < i, u = e < n, e < s);
                var a, p, d, f, l, g, c = h + u + o;
                return 1 === c ? h ? (a = (e - i) / (n - i), p = (e - i) / (s - i), r.push(1), r.push(2), 1 !== p && (r.push(-1), r.push(0), r.push(2), r.push(p)), 1 !== a && (r.push(-1), r.push(0), r.push(1), r.push(a))) : u ? (d = (e - n) / (s - n), f = (e - n) / (i - n), r.push(2), r.push(0), 1 !== f && (r.push(-1), r.push(1), r.push(0), r.push(f)), 1 !== d && (r.push(-1), r.push(1), r.push(2), r.push(d))) : o && (l = (e - s) / (i - s), g = (e - s) / (n - s), r.push(0), r.push(1), 1 !== g && (r.push(-1), r.push(2), r.push(1), r.push(g)), 1 !== l && (r.push(-1), r.push(2), r.push(0), r.push(l))) : 2 === c ? h || i === e ? u || n === e ? o || s === e || (p = (e - i) / (s - i), d = (e - n) / (s - n), r.push(2), r.push(-1), r.push(0), r.push(2), r.push(p), r.push(-1), r.push(1), r.push(2), r.push(d)) : (g = (e - s) / (n - s), a = (e - i) / (n - i), r.push(1), r.push(-1), r.push(2), r.push(1), r.push(g), r.push(-1), r.push(0), r.push(1), r.push(a)) : (f = (e - n) / (i - n), l = (e - s) / (i - s), r.push(0), r.push(-1), r.push(1), r.push(0), r.push(f), r.push(-1), r.push(2), r.push(0), r.push(l)) : 3 !== c && (r.push(0), r.push(1), r.push(2)), r
            }, computeBarycentricCoordinates: function (e, t, i, n, s, r, h, u, o) {
                var a = i - h, p = h - s, d = r - u, f = n - u, l = 1 / (d * a + p * f), g = t - u, c = e - h,
                    m = (d * c + p * g) * l, x = (-f * c + a * g) * l, v = 1 - m - x;
                return w.defined(o) ? (o.x = m, o.y = x, o.z = v, o) : new ce.Cartesian3(m, x, v)
            }, computeLineSegmentLineSegmentIntersection: function (e, t, i, n, s, r, h, u, o) {
                var a = (u - r) * (i - e) - (h - s) * (n - t);
                if (0 != a) {
                    var p = ((h - s) * (t - r) - (u - r) * (e - s)) / a, d = ((i - e) * (t - r) - (n - t) * (e - s)) / a;
                    return 0 <= p && p <= 1 && 0 <= d && d <= 1 ? (w.defined(o) || (o = new ce.Cartesian2), o.x = e + p * (i - e), o.y = t + p * (n - t), o) : void 0
                }
            }
        }, ye = 32767, Be = 16383, Ie = [], Ae = [], be = [], Te = new ce.Cartographic, ze = new ce.Cartesian3, Me = [],
        Ne = [], Ve = [], Ee = [], Re = [], He = new ce.Cartesian3, Oe = new me.BoundingSphere,
        Se = new ve.OrientedBoundingBox, Ue = new ce.Cartesian2, Fe = new ce.Cartesian3;

    function Pe() {
        this.vertexBuffer = void 0, this.index = void 0, this.first = void 0, this.second = void 0, this.ratio = void 0
    }

    Pe.prototype.clone = function (e) {
        return w.defined(e) || (e = new Pe), e.uBuffer = this.uBuffer, e.vBuffer = this.vBuffer, e.heightBuffer = this.heightBuffer, e.normalBuffer = this.normalBuffer, e.index = this.index, e.first = this.first, e.second = this.second, e.ratio = this.ratio, e
    }, Pe.prototype.initializeIndexed = function (e, t, i, n, s) {
        this.uBuffer = e, this.vBuffer = t, this.heightBuffer = i, this.normalBuffer = n, this.index = s, this.first = void 0, this.second = void 0, this.ratio = void 0
    }, Pe.prototype.initializeFromClipResult = function (e, t, i) {
        var n = t + 1;
        return -1 !== e[t] ? i[e[t]].clone(this) : (this.vertexBuffer = void 0, this.index = void 0, this.first = i[e[n]], ++n, this.second = i[e[n]], ++n, this.ratio = e[n], ++n), n
    }, Pe.prototype.getKey = function () {
        return this.isIndexed() ? this.index : JSON.stringify({
            first: this.first.getKey(),
            second: this.second.getKey(),
            ratio: this.ratio
        })
    }, Pe.prototype.isIndexed = function () {
        return w.defined(this.index)
    }, Pe.prototype.getH = function () {
        return w.defined(this.index) ? this.heightBuffer[this.index] : ge.CesiumMath.lerp(this.first.getH(), this.second.getH(), this.ratio)
    }, Pe.prototype.getU = function () {
        return w.defined(this.index) ? this.uBuffer[this.index] : ge.CesiumMath.lerp(this.first.getU(), this.second.getU(), this.ratio)
    }, Pe.prototype.getV = function () {
        return w.defined(this.index) ? this.vBuffer[this.index] : ge.CesiumMath.lerp(this.first.getV(), this.second.getV(), this.ratio)
    };
    var a = new ce.Cartesian2, p = -1, d = [new ce.Cartesian3, new ce.Cartesian3],
        f = [new ce.Cartesian3, new ce.Cartesian3];

    function l(e, t) {
        var i = d[++p], n = f[p];
        return i = s.AttributeCompression.octDecode(e.first.getNormalX(), e.first.getNormalY(), i), n = s.AttributeCompression.octDecode(e.second.getNormalX(), e.second.getNormalY(), n), ze = ce.Cartesian3.lerp(i, n, e.ratio, ze), ce.Cartesian3.normalize(ze, ze), s.AttributeCompression.octEncode(ze, t), --p, t
    }

    Pe.prototype.getNormalX = function () {
        return w.defined(this.index) ? this.normalBuffer[2 * this.index] : (a = l(this, a)).x
    }, Pe.prototype.getNormalY = function () {
        return w.defined(this.index) ? this.normalBuffer[2 * this.index + 1] : (a = l(this, a)).y
    };
    var c = [];

    function ke(e, t, i, n, s, r, h, u, o) {
        if (0 !== h.length) {
            for (var a = 0, p = 0; p < h.length;) p = c[a++].initializeFromClipResult(h, p, u);
            for (var d = 0; d < a; ++d) {
                var f = c[d];
                if (f.isIndexed()) f.newIndex = r[f.index], f.uBuffer = e, f.vBuffer = t, f.heightBuffer = i, o && (f.normalBuffer = n); else {
                    var l = f.getKey();
                    if (w.defined(r[l])) f.newIndex = r[l]; else {
                        var g = e.length;
                        e.push(f.getU()), t.push(f.getV()), i.push(f.getH()), o && (n.push(f.getNormalX()), n.push(f.getNormalY())), f.newIndex = g, r[l] = g
                    }
                }
            }
            3 === a ? (s.push(c[0].newIndex), s.push(c[1].newIndex), s.push(c[2].newIndex)) : 4 === a && (s.push(c[0].newIndex), s.push(c[1].newIndex), s.push(c[2].newIndex), s.push(c[0].newIndex), s.push(c[2].newIndex), s.push(c[3].newIndex))
        }
    }

    return c.push(new Pe), c.push(new Pe), c.push(new Pe), c.push(new Pe), u(function (e, t) {
        var i = e.isEastChild, n = e.isNorthChild, s = i ? Be : 0, r = i ? ye : Be, h = n ? Be : 0, u = n ? ye : Be,
            o = Me, a = Ne, p = Ve, d = Re;
        o.length = 0, a.length = 0, p.length = 0, d.length = 0;
        var f = Ee;
        f.length = 0;
        var l = {}, g = e.vertices, c = e.indices;
        c = c.subarray(0, e.indexCountWithoutSkirts);
        var m, x, v, w, C, y = we.TerrainEncoding.clone(e.encoding), B = y.hasVertexNormals, I = e.exaggeration, A = 0,
            b = e.vertexCountWithoutSkirts, T = e.minimumHeight, z = e.maximumHeight, M = new Array(b),
            N = new Array(b), V = new Array(b), E = B ? new Array(2 * b) : void 0;
        for (v = x = 0; x < b; ++x, v += 2) {
            var R = y.decodeTextureCoordinates(g, x, Ue);
            if (m = y.decodeHeight(g, x) / I, w = ge.CesiumMath.clamp(R.x * ye | 0, 0, ye), C = ge.CesiumMath.clamp(R.y * ye | 0, 0, ye), V[x] = ge.CesiumMath.clamp((m - T) / (z - T) * ye | 0, 0, ye), w < 20 && (w = 0), C < 20 && (C = 0), ye - w < 20 && (w = ye), ye - C < 20 && (C = ye), M[x] = w, N[x] = C, B) {
                var H = y.getOctEncodedNormal(g, x, Fe);
                E[v] = H.x, E[v + 1] = H.y
            }
            (i && Be <= w || !i && w <= Be) && (n && Be <= C || !n && C <= Be) && (l[x] = A, o.push(w), a.push(C), p.push(V[x]), B && (d.push(E[v]), d.push(E[v + 1])), ++A)
        }
        var O = [];
        O.push(new Pe), O.push(new Pe), O.push(new Pe);
        var S, U = [];
        for (U.push(new Pe), U.push(new Pe), U.push(new Pe), x = 0; x < c.length; x += 3) {
            var F = c[x], P = c[x + 1], k = c[x + 2], D = M[F], W = M[P], X = M[k];
            O[0].initializeIndexed(M, N, V, E, F), O[1].initializeIndexed(M, N, V, E, P), O[2].initializeIndexed(M, N, V, E, k);
            var K = Ce.clipTriangleAtAxisAlignedThreshold(Be, i, D, W, X, Ie);
            (S = 0) >= K.length || (S = U[0].initializeFromClipResult(K, S, O)) >= K.length || (S = U[1].initializeFromClipResult(K, S, O)) >= K.length || (S = U[2].initializeFromClipResult(K, S, O), ke(o, a, p, d, f, l, Ce.clipTriangleAtAxisAlignedThreshold(Be, n, U[0].getV(), U[1].getV(), U[2].getV(), Ae), U, B), S < K.length && (U[2].clone(U[1]), U[2].initializeFromClipResult(K, S, O), ke(o, a, p, d, f, l, Ce.clipTriangleAtAxisAlignedThreshold(Be, n, U[0].getV(), U[1].getV(), U[2].getV(), Ae), U, B)))
        }
        var L = i ? -ye : 0, Y = n ? -ye : 0, _ = [], G = [], J = [], Z = [], j = Number.MAX_VALUE, q = -j, Q = be;
        Q.length = 0;
        var $ = ce.Ellipsoid.clone(e.ellipsoid), ee = ce.Rectangle.clone(e.childRectangle), te = ee.north,
            ie = ee.south, ne = ee.east, se = ee.west;
        for (ne < se && (ne += ge.CesiumMath.TWO_PI), x = 0; x < o.length; ++x) w = (w = Math.round(o[x])) <= s ? (_.push(x), 0) : r <= w ? (J.push(x), ye) : 2 * w + L, o[x] = w, C = (C = Math.round(a[x])) <= h ? (G.push(x), 0) : u <= C ? (Z.push(x), ye) : 2 * C + Y, a[x] = C, (m = ge.CesiumMath.lerp(T, z, p[x] / ye)) < j && (j = m), q < m && (q = m), p[x] = m, Te.longitude = ge.CesiumMath.lerp(se, ne, w / ye), Te.latitude = ge.CesiumMath.lerp(ie, te, C / ye), Te.height = m, $.cartographicToCartesian(Te, ze), Q.push(ze.x), Q.push(ze.y), Q.push(ze.z);
        var re = me.BoundingSphere.fromVertices(Q, ce.Cartesian3.ZERO, 3, Oe),
            he = ve.OrientedBoundingBox.fromRectangle(ee, j, q, $, Se),
            ue = new we.EllipsoidalOccluder($).computeHorizonCullingPointFromVerticesPossiblyUnderEllipsoid(re.center, Q, 3, re.center, j, He),
            oe = q - j, ae = new Uint16Array(o.length + a.length + p.length);
        for (x = 0; x < o.length; ++x) ae[x] = o[x];
        var pe = o.length;
        for (x = 0; x < a.length; ++x) ae[pe + x] = a[x];
        for (pe += a.length, x = 0; x < p.length; ++x) ae[pe + x] = ye * (p[x] - j) / oe;
        var de, fe = xe.IndexDatatype.createTypedArray(o.length, f);
        if (B) {
            var le = new Uint8Array(d);
            t.push(ae.buffer, fe.buffer, le.buffer), de = le.buffer
        } else t.push(ae.buffer, fe.buffer);
        return {
            vertices: ae.buffer,
            encodedNormals: de,
            indices: fe.buffer,
            minimumHeight: j,
            maximumHeight: q,
            westIndices: _,
            southIndices: G,
            eastIndices: J,
            northIndices: Z,
            boundingSphere: re,
            orientedBoundingBox: he,
            horizonOcclusionPoint: ue
        }
    })
});
