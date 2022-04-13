package br.unifesspa.edu.br;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.TimerTask;

public class UDPClient {
	
	public static void normal() {
		System.out.print("Insira a quantidade de clientes que deseja criar: ");
		Scanner in = new Scanner(System.in);
		int qtde = in.nextInt();
		ArrayList<Cliente> clientes = new ArrayList<>();
		ArrayList<Thread> threads = new ArrayList<>();
		for(int i = 0; i < qtde; i++) {
			Cliente cliente = new Cliente("Cliente "+String.valueOf(i), "localhost", 8000);
			clientes.add(cliente);
			Thread t = new Thread(cliente);
			threads.add(t);
			t.start();
		}
		
		for (Thread t : threads) try {t.join();} catch (InterruptedException e) {e.printStackTrace();}
		
		for (int i = 0; i < qtde; i++) {
			System.out.println("\nRespostas enviadas pelo cliente {"+clientes.get(i).getName()+"}");
			for (String resp : clientes.get(i).getRespostas()) System.out.println(resp);
			System.out.println("Gabarito: ");
			for (String gab : clientes.get(i).getGabarito()) System.out.println(gab);
		}
		in.close();
	}
	
	public static void demonstracaoTimeOut() {
		try {
			DatagramSocket s = new DatagramSocket();
			System.out.println("Enviando uma resposta...");
			String resposta = "1;5;FFFFF";
			System.out.println(resposta);
			DatagramPacket req = new DatagramPacket(resposta.getBytes(), resposta.length(), InetAddress.getByName("localhost"), 8000);
			s.send(req);
			
			//aguardando resposta
			System.out.println("Aguardando a sessão expirar...");
			byte[] buffer = new byte[1024];
			DatagramPacket resp = new DatagramPacket(buffer, buffer.length);
			s.receive(resp);
			System.out.println(new String(buffer));
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.out.print("Insira o modo (normal, timeout): ");
		Scanner in = new Scanner(System.in);
		String modo = in.nextLine();
		
		switch (modo) {
			case "timeout": demonstracaoTimeOut(); break;
			case "normal": normal(); break;
			default: System.out.println("Modo inválido...") ; break;
		}
		
		System.out.println("Saindo da aplicação...");
		
	}
}

class Cliente implements Runnable {
	private final int PORT;
	private String name;
	private DatagramSocket ds;
	private InetAddress address;
	private Random rng;
	private ArrayList<String> respostas = new ArrayList<>();
	private ArrayList<String> gabarito = new ArrayList<>();

	Cliente(String name, String host, int porta) {
		this.setRng(new Random());
		this.setName(name);
		this.PORT = porta;
		try {
			this.setDs(new DatagramSocket());
			this.setAddress(InetAddress.getByName(host));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private synchronized String respostaAleatoria() {
		String[] vf = { "V", "F" };
		String respostas = "";
		for (int i = 0; i < 5; i++) {
			respostas += vf[this.rng.nextInt(vf.length)];
		}
		return respostas;
	}

	@Override
	public void run() {
		for (int i = 0; i < 5; i++) {
			String resposta = String.valueOf(i + 1) + ";5;" + this.respostaAleatoria();
			this.getRespostas().add(resposta);
			DatagramPacket req = new DatagramPacket(resposta.getBytes(), resposta.length(), this.getAddress(),
					this.getPORT());
			try {this.ds.send(req);} catch (IOException e) {e.printStackTrace();}
		}
		
		for (int i = 0; i < 5; i++) {
			byte[] buffer = new byte[1024];
			DatagramPacket resp = new DatagramPacket(buffer, buffer.length);
			try {this.getDs().receive(resp);} catch (IOException e) {e.printStackTrace();}
			this.getGabarito().add(new String(buffer));
		}
		this.getDs().close();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPORT() {
		return PORT;
	}

	public InetAddress getAddress() {
		return address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public DatagramSocket getDs() {
		return ds;
	}

	public void setDs(DatagramSocket ds) {
		this.ds = ds;
	}

	public Random getRng() {
		return rng;
	}

	public void setRng(Random rng) {
		this.rng = rng;
	}

	public ArrayList<String> getRespostas() {
		return respostas;
	}

	public void setRespostas(ArrayList<String> respostas) {
		this.respostas = respostas;
	}

	public ArrayList<String> getGabarito() {
		return gabarito;
	}

	public void setGabarito(ArrayList<String> gabarito) {
		this.gabarito = gabarito;
	}
}