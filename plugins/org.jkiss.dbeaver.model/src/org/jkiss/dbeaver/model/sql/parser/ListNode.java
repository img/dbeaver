/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
 */
package org.jkiss.dbeaver.model.sql.parser;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Represents node of single-linked-list without any unwanted computational cost.
 * Takes 0 resources to represent empty list with NULL value.
 * @param <T>
 */
public class ListNode<T> implements Iterable<T> {
    public final ListNode<T> next;
    public final T data;

    private ListNode(ListNode<T> next, T data) {
        this.next = next;
        this.data = data;
    }

    public static <T> ListNode<T> of(T data) {
        return new ListNode<T>(null, data);
    }

    public static <T> ListNode<T> of(T data1, T data2) {
        return new ListNode<T>(new ListNode<T>(null, data1), data2);
    }

    public static <T> ListNode<T> push(ListNode<T> node, T data) {
        return new ListNode<T>(node, data);
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private ListNode<T> node = ListNode.this;

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            public T next() {
                if (node != null) {
                    T result = node.data;
                    node = node.next;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
