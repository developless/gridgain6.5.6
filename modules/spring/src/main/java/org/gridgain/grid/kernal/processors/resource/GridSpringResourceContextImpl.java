/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.resource;

import org.gridgain.grid.*;
import org.springframework.aop.framework.*;
import org.springframework.context.*;

/**
 * TODO
 */
public class GridSpringResourceContextImpl implements GridSpringResourceContext {
    /** Spring application context injector. */
    private GridResourceInjector springCtxInjector;

    /** Spring bean resources injector. */
    private GridResourceInjector springBeanInjector;

    /**
     * @param springCtx Spring application context.
     */
    public GridSpringResourceContextImpl(ApplicationContext springCtx) {
        springCtxInjector = new GridResourceBasicInjector<>(springCtx);
        springBeanInjector = new GridResourceSpringBeanInjector(springCtx);
    }

    @Override public GridResourceInjector springBeanInjector() {
        return springBeanInjector;
    }

    @Override public GridResourceInjector springContextInjector() {
        return springCtxInjector;
    }

    @Override public Object unwrapTarget(Object target) throws GridException {
        if (target instanceof Advised) {
            try {
                return ((Advised)target).getTargetSource().getTarget();
            }
            catch (Exception e) {
                throw new GridException("Failed to unwrap Spring proxy target [cls=" + target.getClass().getName() +
                    ", target=" + target + ']', e);
            }
        }

        return target;
    }
}