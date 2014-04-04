/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.cache.*;

/**
 * Tests for local cache.
 */
public class GridCachePartitionedNearOnlyNoPrimaryFullApiSelfTest
    extends GridCachePartitionedClientOnlyNoPrimaryFullApiSelfTest {
    /** {@inheritDoc} */
    @Override protected GridCacheDistributionMode distributionMode() {
        return GridCacheDistributionMode.NEAR_ONLY;
    }
}