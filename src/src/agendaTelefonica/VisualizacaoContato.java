// VisualizacaoContato.java
// Atividade para a visualização de um único contato.
package src.agendaTelefonica;

import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import com.agendaTelefonica.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class VisualizacaoContato extends Activity 
{
	private static final String TAG = VisualizacaoContato.class.getName();

	// Código de pedido Intenção usado para iniciar uma atividade que retorna um resultado
	private static final int REQUEST_CONNECT_DEVICE = 1;

	private BluetoothAdapter bluetoothAdapter = null; // Declaracao de uma variavel BluetoothAdapter
	private Handler handler; // Handler para a exibição de Toasts no segmento de GUI 
	private long rowID; // ID do contato selecionado na linha de identificação do Banco de Dados 
	private TextView nameTextView; // Exibe o nome do contato
	private TextView phoneTextView; // Exibe o telefone de contato 
	private TextView emailTextView; // Exibe um contato de e-mail 
	private TextView streetTextView; // Exibe rua do contato
	private TextView cityTextView; // Exibe cidade/estado/zip do contato

	// Chamado quando a atividade é criada pela primeira vez
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_contact); // inflate GUI

		// Obtém os EditTexts
		nameTextView = (TextView) findViewById(R.id.nameTextView);
		phoneTextView = (TextView) findViewById(R.id.phoneTextView);
		emailTextView = (TextView) findViewById(R.id.emailTextView);
		streetTextView = (TextView) findViewById(R.id.streetTextView);
		cityTextView = (TextView) findViewById(R.id.cityTextView);

		// Obtém o contato selecionado linha ID
		Bundle extras = getIntent().getExtras();
		rowID = extras.getLong(AgendaTelefonica.ROW_ID); 

		// Obtém o adaptador Bluetooth padrão
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		handler = new Handler(); // Criacao do Handler
	} // Fim do metodo onCreate

	// Chamado quando a atividade é criada pela primeira vez
	@Override
	protected void onResume()
	{
		super.onResume();

		// Cria novo LoadContactTask e executá-lo
		new LoadContactTask().execute(rowID);
	} // Fim do metodo onResume

	// Realiza consulta de banco de dados fora do thread de GUI
	private class LoadContactTask extends AsyncTask<Long, Object, Cursor> 
	{
		ConexaoBancoDados databaseConnector = 
				new ConexaoBancoDados(VisualizacaoContato.this);

		// Executar o acesso ao banco
		@Override
		protected Cursor doInBackground(Long... params)
		{
			databaseConnector.open();

			// Obtém um cursor que contém todos os dados sobre determinada entrada
			return databaseConnector.getOneContact(params[0]);
		} // Fim do metodo doInBackground

		// Usar o cursor retornado do método doInBackground
		@Override
		protected void onPostExecute(Cursor result)
		{
			super.onPostExecute(result);

			result.moveToFirst(); // Mover para o primeiro item

			// Obtém o índice da coluna para cada item de dados
			int nameIndex = result.getColumnIndex("name");
			int phoneIndex = result.getColumnIndex("phone");
			int emailIndex = result.getColumnIndex("email");
			int streetIndex = result.getColumnIndex("street");
			int cityIndex = result.getColumnIndex("city");

			// Preenchendo TextViews com os dados recuperados
			nameTextView.setText(result.getString(nameIndex));
			phoneTextView.setText(result.getString(phoneIndex));
			emailTextView.setText(result.getString(emailIndex));
			streetTextView.setText(result.getString(streetIndex));
			cityTextView.setText(result.getString(cityIndex));

			result.close(); // Fecha o result
			databaseConnector.close(); // Fecha a conexão com o banco
		} // Fim do metodo onPostExecute
	} // Fim da classe LoadContactTask

	// Cria o menu da Atividade de um arquivo XML recurso de menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_contact_menu, menu);
		return true;
	} // Fim do metodo onCreateOptionsMenu

	// Controle de Escolha de menu de opções
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) // Switch baseado em selecionado ID do MenuItem
		{
		case R.id.editItem: // Selecionado Editar Contato item de menu
			// Cria a intenção de lançar o AddEditContact Atividade
			Intent addEditContact =
			new Intent(this, AdicionarEditarContato.class);

			// Passa os dados do contato selecionado como extras, com a intenção
			addEditContact.putExtra(AgendaTelefonica.ROW_ID, rowID);
			addEditContact.putExtra("name", nameTextView.getText());
			addEditContact.putExtra("phone", phoneTextView.getText());
			addEditContact.putExtra("email", emailTextView.getText());
			addEditContact.putExtra("street", streetTextView.getText());
			addEditContact.putExtra("city", cityTextView.getText());
			startActivity(addEditContact); // Inicia o Activity
			break;
		case R.id.deleteItem: // Selecionado Excluir contato item de menu
			deleteContact(); // Exclui o contato exibido
			break;
		case R.id.transferItem: // Selecionado Transferência contato item de menu
			// Se não estiver conectado
			if (bluetoothAdapter.isEnabled()) 
			{
				// Lançamento DeviceChooser assim o usuário pode escolher um dispositivo próximo
				Intent serverIntent = 
						new Intent(this, SeletorDispositivo.class);
				startActivityForResult(
						serverIntent, REQUEST_CONNECT_DEVICE);
			} // Fim do if
			else // Indica que o Bluetooth não está habilitado
			{
				Toast.makeText(this, 
						R.string.no_bluetooth, Toast.LENGTH_LONG).show();
			} // Fim do else
			break;
		} // Fim do switch

		return super.onOptionsItemSelected(item);
	} // Fim do método onOptionsItemSelected

	// Apaga um contato
	private void deleteContact()
	{
		// Cria um novo AlertDialog Builder
		AlertDialog.Builder builder = 
				new AlertDialog.Builder(VisualizacaoContato.this);

		builder.setTitle(R.string.confirmTitle); // Barra de titulo String
		builder.setMessage(R.string.confirmMessage); // Mensagem para ser exibida

		// Fornece um botão OK que simplesmente descarta o diálogo
		builder.setPositiveButton(R.string.button_delete,
				new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int button)
			{
				final ConexaoBancoDados databaseConnector = 
						new ConexaoBancoDados(VisualizacaoContato.this);
				
				// Cria um AsyncTask que exclui o contato em outro
				// Thread, em seguida, chama de chegada após a eliminação
				AsyncTask<Long, Object, Object> deleteTask =
						new AsyncTask<Long, Object, Object>()
						{
					@Override
					protected Object doInBackground(Long... params)
					{
						databaseConnector.deleteContact(params[0]); 
						return null;
					} // Fim do metodo doInBackground

					@Override
					protected void onPostExecute(Object result)
					{
						finish(); // Retorno ao AgendaTelefonica Activity
					} // Fim do metodo onPostExecute
						}; // Fim do novo AsyncTask
						
						// Executa o AsyncTask excluir contato na rowID
						deleteTask.execute(new Long[] { rowID });               
			} // Fim do método onClick
		} // Fim da classe interna anônima
				); // Fim para o método setPositiveButton

		builder.setNegativeButton(R.string.button_cancel, null);
		builder.show(); // Exibe a caixa de diálogo
	} // Fim do metodo DeleteContact

	// Chamado quando uma Atividade lançado a partir de este usando
	// Termina startActivityForResult
	@SuppressLint("ShowToast")
	public void onActivityResult(int requestCode, int resultCode, 
			Intent data) 
	{
		// Se a conexão foi estabelecida
		if (resultCode == Activity.RESULT_OK) 
		{
			// Obtém o endereço MAC do dispositivo remoto e passá-lo para que 
			// SendContactTask do método execute
			new SendContactTask().execute(new String[] { 
					data.getExtras().getString(SeletorDispositivo.DEVICE_ADDRESS)});
		} // Fim do if
		else // Houve um erro de conexão
		{
			Toast.makeText(this, 
					R.string.connection_error, Toast.LENGTH_LONG);
		} // end else
	} // Fim do metodo onActivityResult

	// Tarefa para o envio de um contato em uma discussão de fundo
	private class SendContactTask extends AsyncTask<String, Object, Object>
	{
		// Obtém o BluetoothDevice para o endereço especificado,
		// Conecta ao dispositivo e enviar o contato
		@Override
		protected Object doInBackground(String... params)
		{
			// Obtém um objeto BluetoothDevice representando o dispositivo remoto
			BluetoothDevice device =
					bluetoothAdapter.getRemoteDevice(params[0]);

			BluetoothSocket bluetoothSocket = null; // Para o envio de contato

			// Fazer a conexão ao dispositivo remoto e enviar contato
			try 
			{
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.sending_contact);

				// Obtém BluetoothSocket, em seguida, ligue para o outro dispositivo
				bluetoothSocket = device.createRfcommSocketToServiceRecord(
						AgendaTelefonica.MY_UUID);
				bluetoothSocket.connect(); // Estabelecer a conexão

				// Obtém fluxos de comunicação via BluetoothSocket
				OutputStream outputStream = bluetoothSocket.getOutputStream();
				
				// Cria JSONObject representando o contato
				final JSONObject contact = new JSONObject();
				contact.put("name", nameTextView.getText().toString());
				contact.put("phone", phoneTextView.getText().toString());
				contact.put("email", emailTextView.getText().toString());
				contact.put("street", streetTextView.getText().toString());
				contact.put("city", cityTextView.getText().toString());

				// Envia uma matriz de bytes que contém as informações do contato
				outputStream.write(contact.toString().getBytes()); 
				outputStream.flush();
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.contact_sent);
			} // Fim do try
			catch (IOException e) // Problema enviando contato
			{
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.transfer_failed);
				Log.e(TAG, e.toString());
			} // Fim do catch
			catch (JSONException e) // Problema com formato de arquivo JSON
			{
				AgendaTelefonica.displayToastViaHandler(VisualizacaoContato.this, handler, 
						R.string.transfer_failed);
				Log.e(TAG, e.toString());
			} // Fim do catch
			finally // Garantir que BluetoothSocket está fechado
			{
				try
				{
					bluetoothSocket.close(); // Fecha BluetoothSocket
				} // Fim do try
				catch (IOException e) // Problema fechando BluetoothSocket
				{
					Log.e(TAG, e.toString());
				} // Fim do catch

				bluetoothSocket = null;
			} // Fim do finally

			return null;
		} // Fim do metodo doInBackground
	} // Fim da classe SendContactTask 
} // Fim da classe ViewContact