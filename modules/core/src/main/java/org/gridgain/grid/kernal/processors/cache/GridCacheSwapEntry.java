/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.portables.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * Swap entry.
 */
public interface GridCacheSwapEntry<V> {
    /**
     * @return Key hash.
     */
    public int keyHash();

    /**
     * @return Value bytes.
     */
    public byte[] valueBytes();

    /**
     * @return Value.
     */
    public V value();

    /**
     * @param val Value.
     */
    void value(V val);

    /**
     * @return Whether value is byte array.
     */
    public boolean valueIsByteArray();

    /**
     * @return Version.
     */
    public GridCacheVersion version();

    /**
     * @return Time to live.
     */
    public long ttl();

    /**
     * @return Expire time.
     */
    public long expireTime();

    /**
     * @return Class loader ID for entry key ({@code null} for local class loader).
     */
    @Nullable public GridUuid keyClassLoaderId();

    /**
     * @return Class loader ID for entry value ({@code null} for local class loader).
     */
    @Nullable public GridUuid valueClassLoaderId();

    /**
     * @return If entry is offheap based returns tuple where first value is pointer and second is value size.
     */
    @Nullable GridBiTuple<Long, Integer> offheapPointer();
}
