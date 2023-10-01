package pt.ulisboa.tecnico.classes.classserver;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.classes.contract.ClassesDefinitions.ResponseCode;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer;
import pt.ulisboa.tecnico.classes.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.ActivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DeactivateGossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.GossipResponse;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpRequest;
import pt.ulisboa.tecnico.classes.contract.admin.AdminClassServer.DumpResponse;


public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

    // Internal Class State
    final private Class _class;

    /**
     * Constructor
     * @param _class
     */
    public AdminServiceImpl(Class _class) {
        this._class = _class;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        ClassServer.debug("ADMIN: Activate Request");

        // Activate Server
        ClassServer.activateServer();

        // Build and send response
        responseObserver.onNext(ActivateResponse.newBuilder().setCode(ResponseCode.OK).build());
        responseObserver.onCompleted();

    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        ClassServer.debug("ADMIN: Deactivate Request");

        // Deactivate Server
        ClassServer.deactivateServer();

        // Build and send response
        responseObserver.onNext(DeactivateResponse.newBuilder().setCode(ResponseCode.OK).build());
        responseObserver.onCompleted();

    }

    @Override
    public void dump(DumpRequest request, StreamObserver<DumpResponse> responseObserver) {
        ClassServer.debug("ADMIN: Dump Request");

        // Get class state and build response
        DumpResponse response = DumpResponse.newBuilder().setCode(ResponseCode.OK)
                .setClassState(_class.createClassState()).build();

        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void activateGossip(ActivateGossipRequest request, StreamObserver<ActivateGossipResponse> responseObserver) {
        ClassServer.debug("ADMIN: activateGossip Request");

        // Activate Server
        ClassServer.activateGossip();

        // Get class state and build response
        ActivateGossipResponse response = ActivateGossipResponse.newBuilder().setCode(ResponseCode.OK).build();

        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void deactivateGossip(DeactivateGossipRequest request, StreamObserver<DeactivateGossipResponse> responseObserver) {
        ClassServer.debug("ADMIN: deactivateGossip Request");

        // Activate Server
        ClassServer.deactivateGossip();

        // Get class state and build response
        DeactivateGossipResponse response = DeactivateGossipResponse.newBuilder().setCode(ResponseCode.OK).build();

        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
        ClassServer.debug("ADMIN: gossip Request");

        // Activate Server
        ClassServer.gossip();

        // Get class state and build response
        GossipResponse response = GossipResponse.newBuilder().setCode(ResponseCode.OK).build();

        // Send response
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

}
