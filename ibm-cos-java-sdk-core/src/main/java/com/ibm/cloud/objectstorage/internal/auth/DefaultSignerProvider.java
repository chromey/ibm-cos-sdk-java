/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.ibm.cloud.objectstorage.internal.auth;

import java.net.URI;

import com.ibm.cloud.objectstorage.AmazonWebServiceClient;
import com.ibm.cloud.objectstorage.AmazonWebServiceRequest;
import com.ibm.cloud.objectstorage.Request;
import com.ibm.cloud.objectstorage.auth.Signer;
import com.ibm.cloud.objectstorage.auth.SignerFactory;
import com.ibm.cloud.objectstorage.auth.SignerParams;
import com.ibm.cloud.objectstorage.auth.SignerTypeAware;
import com.ibm.cloud.objectstorage.util.AwsHostNameUtils;

public class DefaultSignerProvider extends SignerProvider {

    private final AmazonWebServiceClient awsClient;
    private final Signer defaultSigner;

    public DefaultSignerProvider(final AmazonWebServiceClient awsClient,
                                 final Signer defaultSigner) {
        this.awsClient = awsClient;
        this.defaultSigner = defaultSigner;
    }

    @Override
    public Signer getSigner(SignerProviderContext context) {
    Request<?> request = context.getRequest();
        if (request == null || shouldUseDefaultSigner(request.getOriginalRequest())) {
            if (context.isRedirect()) {
                return awsClient.getSignerByURI(context.getUri());
            }
            return defaultSigner;
        }

        SignerTypeAware signerTypeAware = (SignerTypeAware) request.getOriginalRequest();
        SignerParams params = new SignerParams(awsClient.getServiceName(), getSigningRegionForRequestURI(request.getEndpoint()));
        return SignerFactory.createSigner(signerTypeAware.getSignerType(), params);
    }

    private boolean shouldUseDefaultSigner(AmazonWebServiceRequest originalRequest) {
        return !(originalRequest instanceof SignerTypeAware) || isSignerOverridden();
    }

    private boolean isSignerOverridden() {
        return awsClient.getSignerOverride() != null;
    }

    private String getSigningRegionForRequestURI(URI uri) {
        String regionName = awsClient.getSignerRegionOverride();
        if (regionName == null) {
            regionName = AwsHostNameUtils.parseRegion(uri.getHost(), awsClient.getServiceName());
        }
        return regionName;
    }
}
