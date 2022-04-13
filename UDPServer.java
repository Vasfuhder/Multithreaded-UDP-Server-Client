package br.unifesspa.edu.br;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class UDPServer {
	public static void main(String[] args) {
		try {
			Server udpServer = Server.factory(8000);
			udpServer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Server {
	private static ArrayList<Server> instances = new ArrayList<>();
	private ArrayList<Sessao> sessoes = new ArrayList<>();
	private DatagramSocket socket = null;
	private boolean running = true;
	
	protected Server(int porta) {
		try {
			this.socket = new DatagramSocket(porta);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public static Server factory(int porta) throws Exception {
		if(Server.instances.size() > 0) throw new Exception("Só é possível instanciar um servidor.");
		else {
			Server instancia = new Server(porta);
			Server.instances.add(instancia);
			return instancia;
		}
	}
	
	public void start() throws IOException {
		System.out.println("Servidor escutando em "+this.getSocket().getLocalSocketAddress());
		while(this.isRunning()) {
			byte[] buffer = new byte[1024];
			DatagramPacket req = new DatagramPacket(buffer, buffer.length);
			this.getSocket().receive(req);
			System.out.println("Request recebido do cliente na porta "+req.getPort());
			
			//verificando se o cliente possui uma sessao ativa
			Sessao s = this.findSession(req.getAddress(), req.getPort());
			if(Objects.isNull(s)) this.adicionaSessao(req, this.getSocket());
			else s.novaResposta(new String(req.getData()));
			
		}
	}
	
	public Sessao findSession(InetAddress address, int port) {
		for(int i = 0; i < this.getSessoes().size(); i++) {
			if((this.getSessoes().get(i).getPorta() == port) && (this.getSessoes().get(i).getAdress().getHostName().equals(address.getHostName()))) {
				return this.getSessoes().get(i);
			}
		}
		return null;
	}
	
	public void adicionaSessao(DatagramPacket req, DatagramSocket ds) {
		Sessao t = new Sessao(req.getAddress(), req.getPort(), new String(req.getData()), ds);
		this.getSessoes().add(t);
		t.start();
	}

	public ArrayList<Sessao> getSessoes() {
		return sessoes;
	}

	public void setSessoes(ArrayList<Sessao> sessoes) {
		this.sessoes = sessoes;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	public void setSocket(DatagramSocket socket) {
		this.socket = socket;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}
}

class Sessao extends Thread {
	private int porta;
	private InetAddress adress;
	private DatagramSocket ds;
	private ArrayList<String> respostas = new ArrayList<>();
	private boolean completed = false;
	
	//timer
	private int segundos = 120;
	private long start = System.currentTimeMillis();
	private long end = start + segundos*1000;
	
	Sessao(InetAddress adress, int porta, String resposta, DatagramSocket ds) {
		this.setPorta(porta);
		this.setAdress(adress);
		this.novaResposta(resposta);
		this.setDs(ds);
	}
	
	public synchronized void novaResposta(String resposta) {
		System.out.println("Adicionando resposta do cliente "+String.valueOf(this.getPorta()));
		this.getRespostas().add(resposta);
		if (this.getRespostas().size() >= Questionario.q.size()) this.setCompeted(true);
	}
	
	public ArrayList<String> calcularGabarito() {
		System.out.println("Calculando gabarito do cliente "+String.valueOf(this.getPorta()));
		ArrayList<String> gabarito = new ArrayList<>();
		for(int i = 0; i < Questionario.q.size(); i++) {
			String[] respostaUser = this.getRespostas().get(i).split(";")[2].split("");
			String[] respostaCerta = Questionario.q.get(i).split(";")[2].split("");
			int acertos = 0;
			
			for(int j = 0; j < respostaUser.length; j++) {
				if (respostaUser[i].equals(respostaCerta[j])) acertos++;
			}
			
			gabarito.add(String.valueOf(i+1)+";"+String.valueOf(acertos)+";"+String.valueOf(respostaCerta.length-acertos));
		}
		return gabarito;
	}
	
	public synchronized void enviarGabarito() {
		System.out.println("Enviando gabarito para o cliente "+String.valueOf(this.getPorta()));
		ArrayList<String> gabarito = this.calcularGabarito();
		for (String i : gabarito) {
			DatagramPacket resp = new DatagramPacket(i.getBytes(), i.length(), this.getAdress(), this.getPorta());
			try {this.getDs().send(resp);} 
			catch (IOException e) {e.printStackTrace();}
		}
	}
	
	public synchronized void timeOut() {
		String msg = "Sua sessao expirou!";
		DatagramPacket resp = new DatagramPacket(msg.getBytes(), msg.length(), this.getAdress(), this.getPorta());
		try{this.getDs().send(resp);} catch (IOException e) {e.printStackTrace();}
	}

	public synchronized int getPorta() {
		return porta;
	}

	public void setPorta(int porta) {
		this.porta = porta;
	}

	public synchronized InetAddress getAdress() {
		return adress;
	}

	public void setAdress(InetAddress adress) {
		this.adress = adress;
	}

	public synchronized ArrayList<String> getRespostas() {
		return respostas;
	}

	public synchronized void setRespostas(ArrayList<String> respostas) {
		this.respostas = respostas;
	}
	
	public DatagramSocket getDs() {
		return ds;
	}

	public void setDs(DatagramSocket ds) {
		this.ds = ds;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompeted(boolean completed) {
		this.completed = completed;
	}

	@Override
	public void run() {
		while(System.currentTimeMillis() < this.end) {
			if(this.isCompleted()) {this.enviarGabarito(); break;}
		}
		if(!this.isCompleted()) this.timeOut();
		
		String mensagem = this.isCompleted() ? "foi finalizada com sucesso" : "expirou";
		System.out.println("Sessão do cliente na porta "+String.valueOf(this.getPorta())+" "+mensagem);
	}
}

class Questionario {
	public static ArrayList<String> q = new ArrayList<>();
	
	static {
		q.add("1;5;VVFFV");
		q.add("1;5;FFFFF");
		q.add("1;5;VVVVV");
		q.add("1;5;FVFVF");
		q.add("1;5;VFVFV");
	}
}