import React, {useContext} from "react";
import {Redirect, Switch} from "react-router-dom";

import {RouteWithLayout} from "./components";
import {Main as MainLayout, Minimal as MinimalLayout} from "./layouts";
import loadable from '@loadable/component';
import {AppContext} from "adapter";

const AccountView = loadable(() => import('./views/Account'));
const MineView = loadable(() => import('./views/Mine'));
const NotFoundView = loadable(() => import('./views/NotFound'));
const SignInView = loadable(() => import('./views/SignIn'));
const SignUpView = loadable(() => import('./views/SignUp'));
const SystemView = loadable(() => import('./views/System'));
const MetricsView = loadable(() => import('./views/Metric'));
// custom
const ProxyView = loadable(() => import('./views/Proxy'));
const OrderDetailView = loadable(() => import('./views/Proxy/OrderDetail'));

const ProductWorkSpaceView = loadable(() => import('./views/Product/WorkSpace'));
const ProductCreateView = loadable(() => import('./views/Product/Editor'));
const ProductView = loadable(() => import('./views/Product'));

const MITMView = loadable(() => import('./views/Mitm'));

const IPSourceWorkSpaceView = loadable(() => import('./views/IPSource/WorkSpace'));
const IPSourceCreateView = loadable(() => import('./views/IPSource/Editor'));
const IPSourceView = loadable(() => import('./views/IPSource'));

const PrivateRoute = ({...rest}) => {
    const {user} = useContext(AppContext);
    return !user.overdue ? (
        <RouteWithLayout {...rest} />
    ) : (
        <Redirect
            to={{
                pathname: "/sign-in"
            }}
        />
    );
};

const Routes = () => {
    return (
        <Switch>
            <Redirect
                exact
                from="/"
                to="/mine"
            />
            <PrivateRoute
                component={AccountView}
                exact
                layout={MainLayout}
                path="/accountList"
            />
            <PrivateRoute
                component={MineView}
                exact
                layout={MainLayout}
                path="/mine"
            />
            <PrivateRoute
                component={SystemView}
                exact
                layout={MainLayout}
                path="/systemSettings"
            />

            <RouteWithLayout
                component={SignInView}
                exact
                layout={MinimalLayout}
                path="/sign-in"
            />
            <RouteWithLayout
                component={SignUpView}
                exact
                layout={MinimalLayout}
                path="/sign-up"
            />
            <RouteWithLayout
                component={NotFoundView}
                exact
                layout={MinimalLayout}
                path="/not-found"
            />
            <PrivateRoute
                component={MetricsView}
                exact
                layout={MainLayout}
                path="/metrics"
            />
            <PrivateRoute
                component={ProxyView}
                exact
                layout={MainLayout}
                path="/proxy"
            />
            <PrivateRoute
                component={OrderDetailView}
                exact
                layout={MainLayout}
                path="/orderDetail/:id"
            />


            {/* product*/}
            <PrivateRoute
                component={ProductView}
                exact
                layout={MainLayout}
                path="/Product"
            />
            <PrivateRoute
                component={ProductWorkSpaceView}
                exact
                layout={MainLayout}
                path="/ProductWorkSpace/:id?"
            />
            <PrivateRoute
                component={ProductCreateView}
                exact
                layout={MainLayout}
                path="/createProduct"
            />

            <PrivateRoute
                component={MITMView}
                exact
                layout={MainLayout}
                path="/mitm"
            />
            <PrivateRoute
                component={IPSourceView}
                exact
                layout={MainLayout}
                path="/ipSource"
            />
            <PrivateRoute
                component={IPSourceWorkSpaceView}
                exact
                layout={MainLayout}
                path="/IpResourceWorkSpace/:ipSourceKey?"
            />
            < PrivateRoute
                component={IPSourceCreateView}
                exact
                layout={MainLayout}
                path="/createIpResource"
            />
            <Redirect to="/not-found"/>
        </Switch>
    );
};

export default Routes;
