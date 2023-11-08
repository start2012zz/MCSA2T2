package mobile.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.sceneview.sample.arcursorplacement.R;
import io.github.sceneview.sample.arcursorplacement.databinding.FragmentNotificationsBinding;
import mobile.ui.notifications.ImageListAdapter;
import mobile.ui.notifications.ListData;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();




//         RecyclerView horizontalImageList = root.findViewById(R.id.horizontal_image_list);
//
//        List<Integer> imageResources = new ArrayList<>();
//        imageResources.add(R.drawable.bed);
//        imageResources.add(R.drawable.bed);
//        imageResources.add(R.drawable.bed);
//        imageResources.add(R.drawable.bed);
//        imageResources.add(R.drawable.bed);
//
//        ImageListAdapter adapter = new ImageListAdapter(imageResources);
//        horizontalImageList.setAdapter(adapter);
//
//        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
//        horizontalImageList.setLayoutManager(layoutManager);





        RecyclerView horizontalImageList = root.findViewById(R.id.horizontal_image_list);

        ArrayList<mobile.ui.notifications.ListData> imageList = new ArrayList<>();
        imageList.add(new ListData(R.drawable.bed, "Bed"));
        imageList.add(new ListData(R.drawable.bed, "Bed"));
        imageList.add(new ListData(R.drawable.bed, "Bed"));

        ImageListAdapter adapter = new ImageListAdapter(imageList);
        horizontalImageList.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        horizontalImageList.setLayoutManager(layoutManager);

//        ArrayList<mobile.ui.notifications.ListData> dataArrayList = new ArrayList<>();
//        int[] imageList = new int[]{R.drawable.bed, R.drawable.bed, R.drawable.bed};
//        String[] nameList = new String[]{"Bed", "Bed", "Bed"};
//
//        for (int i = 0; i < imageList.length; i++) {
//            ListData listData = new ListData(imageList[i], nameList[i]);
//            dataArrayList.add(listData);
//        }
//        ImageListAdapter listAdapter = new ImageListAdapter(requireContext(), dataArrayList);
//        horizontalImageList.setAdapter(listAdapter);
//        horizontalImageList.setClickable(true);

        final TextView textView = binding.textNotification;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}