package nz.co.makereti.chimera;

oneway interface IGarageScanCallback {
	void logToClient(String stuff);
	void onScanResults(String scanResults);
}
