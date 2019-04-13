/** Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 */
'use strict';
Object.defineProperty(exports, '__esModule', {value: true});
exports.IdentitySerializers = (exports.IdentitySerializer = (exports.JsonSerializers = (exports.JsonSerializer = undefined)));

var _LiteBuffer = require('./LiteBuffer');
var _invariant = require('fbjs/lib/invariant');
var _invariant2 = _interopRequireDefault(_invariant);
function _interopRequireDefault(obj) {
  return obj && obj.__esModule ? obj : {default: obj};
}

// JSON serializer
/**
 * A Serializer transforms data between the application encoding used in
 * Payloads and the Encodable type accepted by the transport client.
 */ const JsonSerializer = (exports.JsonSerializer = {
  deserialize: data => {
    let str;
    if (data == null) {
      return null;
    } else if (typeof data === 'string') {
      str = data;
    } else if (_LiteBuffer.LiteBuffer.isBuffer(data)) {
      const buffer = data;
      str = buffer.toString('utf8');
    } else {
      const buffer = _LiteBuffer.LiteBuffer.from(data);
      str = buffer.toString('utf8');
    }
    return JSON.parse(str);
  },
  serialize: JSON.stringify,
});

const JsonSerializers = (exports.JsonSerializers = {
  data: JsonSerializer,
  metadata: JsonSerializer,
});

// Pass-through serializer
const IdentitySerializer = (exports.IdentitySerializer = {
  deserialize: data => {
    (0, _invariant2.default)(
      data == null ||
        typeof data === 'string' ||
        _LiteBuffer.LiteBuffer.isBuffer(data) ||
        data instanceof Uint8Array,
      'RSocketSerialization: Expected data to be a string, Buffer, or ' +
        'Uint8Array. Got `%s`.',
      data
    );

    return data;
  },
  serialize: data => data,
});

const IdentitySerializers = (exports.IdentitySerializers = {
  data: IdentitySerializer,
  metadata: IdentitySerializer,
});
