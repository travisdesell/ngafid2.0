define(["./when-c2e8ef35", "./Check-c4f3a3fc", "./Math-d30358ed", "./RuntimeError-6122571f", "./WebGLConstants-4ae0db90", "./ComponentDatatype-5d3f6452", "./IndexDatatype-e3260434", "./createTaskProcessorWorker"], function (y, e, r, m, t, A, w, n) {
    "use strict";
    var b;

    function l(e, r, t) {
        var n, a = e.num_points(), o = t.num_components(), i = new b.AttributeQuantizationTransform;
        if (i.InitFromAttribute(t)) {
            for (var u = new Array(o), s = 0; s < o; ++s) u[s] = i.min_value(s);
            n = {quantizationBits: i.quantization_bits(), minValues: u, range: i.range(), octEncoded: !1}
        }
        b.destroy(i), (i = new b.AttributeOctahedronTransform).InitFromAttribute(t) && (n = {
            quantizationBits: i.quantization_bits(),
            octEncoded: !0
        }), b.destroy(i);
        var d, c = a * o;
        d = y.defined(n) ? function (e, r, t, n, a) {
            var o, i;
            n.quantizationBits <= 8 ? (i = new b.DracoUInt8Array, o = new Uint8Array(a), r.GetAttributeUInt8ForAllPoints(e, t, i)) : (i = new b.DracoUInt16Array, o = new Uint16Array(a), r.GetAttributeUInt16ForAllPoints(e, t, i));
            for (var u = 0; u < a; ++u) o[u] = i.GetValue(u);
            return b.destroy(i), o
        }(e, r, t, n, c) : function (e, r, t, n) {
            var a, o;
            switch (t.data_type()) {
                case 1:
                case 11:
                    o = new b.DracoInt8Array, a = new Int8Array(n), r.GetAttributeInt8ForAllPoints(e, t, o);
                    break;
                case 2:
                    o = new b.DracoUInt8Array, a = new Uint8Array(n), r.GetAttributeUInt8ForAllPoints(e, t, o);
                    break;
                case 3:
                    o = new b.DracoInt16Array, a = new Int16Array(n), r.GetAttributeInt16ForAllPoints(e, t, o);
                    break;
                case 4:
                    o = new b.DracoUInt16Array, a = new Uint16Array(n), r.GetAttributeUInt16ForAllPoints(e, t, o);
                    break;
                case 5:
                case 7:
                    o = new b.DracoInt32Array, a = new Int32Array(n), r.GetAttributeInt32ForAllPoints(e, t, o);
                    break;
                case 6:
                case 8:
                    o = new b.DracoUInt32Array, a = new Uint32Array(n), r.GetAttributeUInt32ForAllPoints(e, t, o);
                    break;
                case 9:
                case 10:
                    o = new b.DracoFloat32Array, a = new Float32Array(n), r.GetAttributeFloatForAllPoints(e, t, o)
            }
            for (var i = 0; i < n; ++i) a[i] = o.GetValue(i);
            return b.destroy(o), a
        }(e, r, t, c);
        var f = A.ComponentDatatype.fromTypedArray(d);
        return {
            array: d,
            data: {
                componentsPerAttribute: o,
                componentDatatype: f,
                byteOffset: t.byte_offset(),
                byteStride: A.ComponentDatatype.getSizeInBytes(f) * o,
                normalized: t.normalized(),
                quantization: n
            }
        }
    }

    function a(e) {
        var r = new b.Decoder, t = ["POSITION", "NORMAL", "COLOR", "TEX_COORD"];
        if (e.dequantizeInShader) for (var n = 0; n < t.length; ++n) r.SkipAttributeTransform(b[t[n]]);
        var a = e.bufferView, o = new b.DecoderBuffer;
        if (o.Init(e.array, a.byteLength), r.GetEncodedGeometryType(o) !== b.TRIANGULAR_MESH) throw new m.RuntimeError("Unsupported draco mesh geometry type.");
        var i = new b.Mesh, u = r.DecodeBufferToMesh(o, i);
        if (!u.ok() || 0 === i.ptr) throw new m.RuntimeError("Error decoding draco mesh geometry: " + u.error_msg());
        b.destroy(o);
        var s = {}, d = e.compressedAttributes;
        for (var c in d) if (d.hasOwnProperty(c)) {
            var f = d[c], y = r.GetAttributeByUniqueId(i, f);
            s[c] = l(i, r, y)
        }
        var A = {
            indexArray: function (e, r) {
                for (var t = e.num_points(), n = e.num_faces(), a = new b.DracoInt32Array, o = 3 * n, i = w.IndexDatatype.createTypedArray(t, o), u = 0, s = 0; s < n; ++s) r.GetFaceFromMesh(e, s, a), i[u + 0] = a.GetValue(0), i[u + 1] = a.GetValue(1), i[u + 2] = a.GetValue(2), u += 3;
                return b.destroy(a), {typedArray: i, numberOfIndices: o}
            }(i, r), attributeData: s
        };
        return b.destroy(i), b.destroy(r), A
    }

    function o(e) {
        return (y.defined(e.primitive) ? a : function (e) {
            var r = new b.Decoder;
            e.dequantizeInShader && (r.SkipAttributeTransform(b.POSITION), r.SkipAttributeTransform(b.NORMAL));
            var t = new b.DecoderBuffer;
            if (t.Init(e.buffer, e.buffer.length), r.GetEncodedGeometryType(t) !== b.POINT_CLOUD) throw new m.RuntimeError("Draco geometry type must be POINT_CLOUD.");
            var n = new b.PointCloud, a = r.DecodeBufferToPointCloud(t, n);
            if (!a.ok() || 0 === n.ptr) throw new m.RuntimeError("Error decoding draco point cloud: " + a.error_msg());
            b.destroy(t);
            var o = {}, i = e.properties;
            for (var u in i) if (i.hasOwnProperty(u)) {
                var s = i[u], d = r.GetAttributeByUniqueId(n, s);
                o[u] = l(n, r, d)
            }
            return b.destroy(n), b.destroy(r), o
        })(e)
    }

    function i(e) {
        b = e, self.onmessage = n(o), self.postMessage(!0)
    }

    return function (e) {
        var r = e.data.webAssemblyConfig;
        if (y.defined(r)) return require([r.modulePath], function (e) {
            y.defined(r.wasmBinaryFile) ? (y.defined(e) || (e = self.DracoDecoderModule), e(r).then(function (e) {
                i(e)
            })) : i(e())
        })
    }
});
