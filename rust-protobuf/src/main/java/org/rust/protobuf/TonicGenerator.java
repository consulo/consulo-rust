/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.protobuf;

import com.intellij.protobuf.lang.psi.PbFile;
import com.intellij.protobuf.lang.psi.PbServiceDefinition;
import com.intellij.protobuf.lang.psi.PbServiceMethod;
import com.intellij.protobuf.lang.psi.PbServiceMethodType;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.generated.RsGeneratedCode;

import java.util.List;

/**
 * The Rust code tonic-build generates for the services of one protobuf package: a {@code <service>_server} module
 * with the service trait and its server, and a {@code <service>_client} module with the client. Like the prost code,
 * the bodies are left out and the parts that only serve the transport are kept to what the signatures need.
 */
final class TonicGenerator {
    @Nonnull
    private final ProtoSymbols symbols;
    @Nonnull
    private final ProstGenerator prost;
    @Nonnull
    private final RsGeneratedCode.Builder out;
    private final boolean server;
    private final boolean client;
    /** {@code tonic::body::Body} since tonic 0.14, {@code tonic::body::BoxBody} before. */
    @Nonnull
    private final String bodyType;

    TonicGenerator(@Nonnull ProtoSymbols symbols, @Nonnull ProstGenerator prost, @Nonnull RsGeneratedCode.Builder out,
                   boolean server, boolean client, @Nonnull String bodyType) {
        this.symbols = symbols;
        this.prost = prost;
        this.out = out;
        this.server = server;
        this.client = client;
        this.bodyType = bodyType;
    }

    void generateFile(@Nonnull PbFile file) {
        String scope = ProtoSymbols.packageScope(file);
        for (PbServiceDefinition service : PsiTreeUtil.getChildrenOfTypeAsList(file, PbServiceDefinition.class)) {
            String name = service.getName();
            if (name == null || service.getBody() == null) continue;
            List<PbServiceMethod> methods = service.getBody().getServiceMethodList();
            if (server) server(service, name, scope, methods);
            if (client) client(service, name, scope, methods);
        }
    }

    private void server(@Nonnull PbServiceDefinition service, @Nonnull String name, @Nonnull String scope,
                        @Nonnull List<PbServiceMethod> methods) {
        String traitName = ProstNames.toUpperCamel(name);
        String serverName = traitName + "Server";
        String module = ProstNames.toSnake(name) + "_server";
        List<String> typePath = List.of(module);
        out.append("/// Generated server implementations.\npub mod ").name(module, service).append(" {\n");
        out.append("    use tonic::codegen::*;\n");
        out.append("    #[async_trait]\n    pub trait ").name(traitName, service)
            .append(": std::marker::Send + std::marker::Sync + 'static {\n");
        for (PbServiceMethod method : methods) {
            String methodName = method.getName();
            if (methodName == null) continue;
            Signature signature = signature(method, scope, typePath);
            String response;
            if (signature.serverStreaming()) {
                String streamName = ProstNames.toUpperCamel(methodName) + "Stream";
                out.append("        /// Server streaming response type for the ").append(methodName).append(" method.\n");
                out.append("        type ").name(streamName, method)
                    .append(": tonic::codegen::tokio_stream::Stream<Item = std::result::Result<")
                    .append(signature.output()).append(", tonic::Status>> + std::marker::Send + 'static;\n");
                response = "Self::" + streamName;
            }
            else {
                response = signature.output();
            }
            String request = signature.clientStreaming() ? "tonic::Streaming<" + signature.input() + ">" : signature.input();
            out.append("        async fn ").name(ProstNames.toSnake(methodName), method)
                .append("(&self, request: tonic::Request<").append(request)
                .append(">) -> std::result::Result<tonic::Response<").append(response).append(">, tonic::Status>;\n");
        }
        out.append("    }\n");

        out.append("    #[derive(Debug)]\n    pub struct ").name(serverName, service).append("<T> {\n        inner: Arc<T>,\n    }\n");
        out.append("    impl<T> ").append(serverName).append("<T> {\n")
            .append("        pub fn new(inner: T) -> Self { loop {} }\n")
            .append("        pub fn from_arc(inner: Arc<T>) -> Self { loop {} }\n")
            .append("        pub fn with_interceptor<F>(inner: T, interceptor: F) -> InterceptedService<Self, F>\n")
            .append("        where\n            F: tonic::service::Interceptor,\n        { loop {} }\n")
            .append("        #[must_use]\n        pub fn accept_compressed(mut self, encoding: CompressionEncoding) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn send_compressed(mut self, encoding: CompressionEncoding) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn max_decoding_message_size(mut self, limit: usize) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn max_encoding_message_size(mut self, limit: usize) -> Self { loop {} }\n")
            .append("    }\n");
        out.append("    impl<T, B> tonic::codegen::Service<http::Request<B>> for ").append(serverName).append("<T>\n")
            .append("    where\n        T: ").append(traitName).append(",\n")
            .append("        B: Body + std::marker::Send + 'static,\n")
            .append("        B::Error: Into<StdError> + std::marker::Send + 'static,\n    {\n")
            .append("        type Response = http::Response<").append(bodyType).append(">;\n")
            .append("        type Error = std::convert::Infallible;\n")
            .append("        type Future = BoxFuture<Self::Response, Self::Error>;\n")
            .append("        fn poll_ready(&mut self, _cx: &mut Context<'_>) -> Poll<std::result::Result<(), Self::Error>> { loop {} }\n")
            .append("        fn call(&mut self, req: http::Request<B>) -> Self::Future { loop {} }\n")
            .append("    }\n");
        out.append("    impl<T> Clone for ").append(serverName).append("<T> {\n        fn clone(&self) -> Self { loop {} }\n    }\n");
        out.append("    /// Generated gRPC service name\n    pub const SERVICE_NAME: &str = \"")
            .append(qualifiedServiceName(scope, name)).append("\";\n");
        out.append("    impl<T> tonic::server::NamedService for ").append(serverName)
            .append("<T> {\n        const NAME: &'static str = SERVICE_NAME;\n    }\n");
        out.append("}\n");
    }

    private void client(@Nonnull PbServiceDefinition service, @Nonnull String name, @Nonnull String scope,
                        @Nonnull List<PbServiceMethod> methods) {
        String clientName = ProstNames.toUpperCamel(name) + "Client";
        String module = ProstNames.toSnake(name) + "_client";
        List<String> typePath = List.of(module);
        out.append("/// Generated client implementations.\npub mod ").name(module, service).append(" {\n");
        out.append("    use tonic::codegen::*;\n    use tonic::codegen::http::Uri;\n");
        out.append("    #[derive(Debug, Clone)]\n    pub struct ").name(clientName, service)
            .append("<T> {\n        inner: tonic::client::Grpc<T>,\n    }\n");
        out.append("    impl ").append(clientName).append("<tonic::transport::Channel> {\n")
            .append("        pub async fn connect<D>(dst: D) -> Result<Self, tonic::transport::Error>\n")
            .append("        where\n            D: TryInto<tonic::transport::Endpoint>,\n            D::Error: Into<StdError>,\n")
            .append("        { loop {} }\n    }\n");
        out.append("    impl<T> ").append(clientName).append("<T>\n    where\n")
            .append("        T: tonic::client::GrpcService<").append(bodyType).append(">,\n")
            .append("        T::Error: Into<StdError>,\n")
            .append("        T::ResponseBody: Body<Data = Bytes> + std::marker::Send + 'static,\n")
            .append("        <T::ResponseBody as Body>::Error: Into<StdError> + std::marker::Send,\n    {\n")
            .append("        pub fn new(inner: T) -> Self { loop {} }\n")
            .append("        pub fn with_origin(inner: T, origin: Uri) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn send_compressed(mut self, encoding: CompressionEncoding) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn accept_compressed(mut self, encoding: CompressionEncoding) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn max_decoding_message_size(mut self, limit: usize) -> Self { loop {} }\n")
            .append("        #[must_use]\n        pub fn max_encoding_message_size(mut self, limit: usize) -> Self { loop {} }\n");
        for (PbServiceMethod method : methods) {
            String methodName = method.getName();
            if (methodName == null) continue;
            Signature signature = signature(method, scope, typePath);
            String request = signature.clientStreaming()
                ? "impl tonic::IntoStreamingRequest<Message = " + signature.input() + ">"
                : "impl tonic::IntoRequest<" + signature.input() + ">";
            String response = signature.serverStreaming()
                ? "tonic::codec::Streaming<" + signature.output() + ">"
                : signature.output();
            out.append("        pub async fn ").name(ProstNames.toSnake(methodName), method)
                .append("(&mut self, request: ").append(request)
                .append(") -> std::result::Result<tonic::Response<").append(response).append(">, tonic::Status> { loop {} }\n");
        }
        out.append("    }\n}\n");
    }

    private record Signature(@Nonnull String input, boolean clientStreaming, @Nonnull String output, boolean serverStreaming) {
    }

    @Nonnull
    private Signature signature(@Nonnull PbServiceMethod method, @Nonnull String scope, @Nonnull List<String> typePath) {
        List<PbServiceMethodType> types = method.getServiceMethodTypeList();
        PbServiceMethodType input = types.size() > 0 ? types.get(0) : null;
        PbServiceMethodType output = types.size() > 1 ? types.get(1) : null;
        return new Signature(
            messageType(input, scope, typePath), input != null && input.isStreaming(),
            messageType(output, scope, typePath), output != null && output.isStreaming()
        );
    }

    @Nonnull
    private String messageType(@Nullable PbServiceMethodType type, @Nonnull String scope, @Nonnull List<String> typePath) {
        if (type == null || type.getMessageTypeName() == null) return "()";
        ProtoSymbols.Symbol symbol = symbols.resolve(type.getMessageTypeName(), scope);
        String reference = type.getMessageTypeName().getReferenceString();
        String fqName = symbol != null ? symbol.fqName() : reference.startsWith(".") ? reference : "." + reference;
        return prost.messagePath(fqName, typePath);
    }

    @Nonnull
    private static String qualifiedServiceName(@Nonnull String scope, @Nonnull String name) {
        return scope.isEmpty() ? name : scope.substring(1) + "." + name;
    }
}
