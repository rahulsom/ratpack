/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.util;

import java.util.Map;

/**
 * Builds a {@link MultiValueMap}.
 *
 * @param <K> the key type of the map
 * @param <V> the value type of the map
 */
public interface MultiValueMapBuilder<K, V> {

  /**
   * Add a value to the map.
   * <p>
   * This method is additive to any previous values added to the key.
   *
   * @param key the key to add the value to
   * @param value the value to add
   * @return this
   */
  MultiValueMapBuilder<K, V> put(K key, V value);

  /**
   * Adds a collection of values to the map.
   * <p>
   * This method is additive to any previous values added to the key.
   *
   * @param key the key to add the values to
   * @param values the values to add
   * @return this
   */
  MultiValueMapBuilder<K, V> putAll(K key, Iterable<? extends V> values);

  /**
   * Adds all the entries in the provided map to this map.
   * <p>
   * The entries in the provided map are additive to any existing values with the same key.
   *
   * @param map the map to merge into this map
   * @return this
   */
  <I extends Iterable<? extends V>> MultiValueMapBuilder<K, V> putAll(Map<K, I> map);

  /**
   * Build the map.
   *
   * @return the multi value map as specified by this builder
   */
  MultiValueMap<K, V> build();
}