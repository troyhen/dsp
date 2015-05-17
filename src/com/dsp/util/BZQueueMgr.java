/* Copyright 2015 Troy D. Heninger
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dsp.util;

import java.util.*;

/**
 * Manages a set of queues.
 */
public class BZQueueMgr {
    static private Map<Object, List<Object>> lists;

    public synchronized static void close() {
        lists = null;
    }

    public static synchronized Object get(Object key) {
        open();
        List<Object> list = lists.get(key);
        if (list == null) return null;
        if (list.size() == 0) return null;
        return list.remove(0);
    }

    public static synchronized CharSequence getAll(Object key) {
        return getSome(key, -1);
    }

    public static synchronized CharSequence getSome(Object key, int max) {
        open();
        List<Object> list = lists.get(key);
        if (list == null) return null;
        int len = list.size();
        if (len == 0) return null;
        if (max < 0) max = len;
        StringBuffer result = new StringBuffer();
        Iterator<Object> it = list.iterator();
        boolean comma = false;
        while (max-- > 0 && it.hasNext()) {
            if (comma) result.append(',');
            result.append(it.next());
            it.remove();
            comma = true;
        }
        return result;
    }

    public synchronized static void open() {
        if (lists == null) lists = new HashMap<Object, List<Object>>();
    }

    public static synchronized void put(Object key, Object value) {
        open();
        List<Object> list = lists.get(key);
        if (list == null) {
            list = new LinkedList<Object>();
            lists.put(key, list);
        }
        if (!list.contains(value)) {
            list.add(value);
        }
    }

    public static synchronized int size(Object key) {
        open();
        List<Object> list = lists.get(key);
        if (list == null) return 0;
        return list.size();
    }

} // BZQueueMgr
